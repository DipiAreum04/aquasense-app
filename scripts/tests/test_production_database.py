"""
Contains production database validation tests.
"""

import json
from time import time
from typing import Iterator

from jsonschema import validate, FormatChecker
from firebase_admin.credentials import Certificate
from firebase_admin.auth import UserRecord, list_users
from firebase_admin import db
import firebase_admin as fb
import pytest


DEVICE_EMAIL_DOMAIN = "@aquasense.ca"
THRESHOLD_KEYS = ("warn_low", "safe_low", "safe_high", "warn_high")

MAX_ACCOUNT_NAME_LENGTH = 100
MAX_ACCOUNT_EMAIL_LENGTH = 254
MAX_AQUARIUM_NAME_LENGTH = 100

BUCKET_COUNT = 100

CLOCK_SKEW_TOLERANCE_SECS = 300


@pytest.fixture(name="firebase_app", scope="session")
def fixture_firebase_app() -> fb.App:
    """Fixture that initializes the production Firebase app once per session."""
    with open("ProdDbAppConfig.json", "r", encoding="utf-8") as app_config_json_file:
        app_config_data = json.load(app_config_json_file)

    certificate = Certificate("ProdDbServiceAccountKey.json")
    return fb.initialize_app(
        certificate,
        options={ "databaseURL": app_config_data["databaseURL"] },
    )


@pytest.fixture(name="production_database", scope="session")
def fixture_production_database(firebase_app: fb.App) -> dict:
    """Fixture that provides the entire production database."""
    database = db.reference(app=firebase_app).get() or {}
    assert isinstance(database, dict)
    return database


@pytest.fixture(name="auth_users", scope="session")
def fixture_auth_users(firebase_app: fb.App) -> dict[str, UserRecord]:
    """Fixture that provides every defined user, keyed by UID."""
    users: dict[str, UserRecord] = {}

    page = list_users(app=firebase_app)
    while page:
        for user in page.users:
            users[user.uid] = user
        page = page.get_next_page()

    return users


@pytest.fixture(name="account_uids", scope="session")
def fixture_account_uids(auth_users: dict[str, UserRecord]) -> set[str]:
    """Fixture that provides the UIDs of every human account."""
    return {
        uid for uid, user in auth_users.items()
        if not (user.email or "").endswith(DEVICE_EMAIL_DOMAIN)
    }


@pytest.fixture(name="device_uids", scope="session")
def fixture_device_uids(auth_users: dict[str, UserRecord]) -> set[str]:
    """Fixture that provides the UIDs of every aquarium device account."""
    return {
        uid for uid, user in auth_users.items()
        if (user.email or "").endswith(DEVICE_EMAIL_DOMAIN)
    }


def bucket_key(index: int) -> str:
    """Returns the bucket key that the given period index refers to."""
    return f"B{index:02d}"


def iterate_aquariums(production_database: dict) -> Iterator[tuple[str, str, dict]]:
    """Yields every `(user_id, aquarium_id, aquarium)` triple in the database."""
    for user_id, user_node in production_database.items():
        for aquarium_id, aquarium in (user_node.get("aquariums") or {}).items():
            yield user_id, aquarium_id, aquarium


def iterate_telemetry(production_database: dict) -> Iterator[tuple[str, str, dict]]:
    """Yields every `(user_id, aquarium_id, telemetry)` triple in the database."""
    for user_id, user_node in production_database.items():
        for aquarium_id, telemetry in (user_node.get("telemetry") or {}).items():
            yield user_id, aquarium_id, telemetry


def iterate_instants(production_database: dict) -> Iterator[tuple[str, dict]]:
    """Yields every `(path, last_instant)` pair under every telemetry sensor."""
    for user_id, aquarium_id, telemetry in iterate_telemetry(production_database):
        for sensor_kind, sensor in telemetry.items():
            instant = sensor.get("last_instant")
            if instant is not None:
                yield f"{user_id}/telemetry/{aquarium_id}/{sensor_kind}/last_instant", instant


def iterate_periods(production_database: dict) -> Iterator[tuple[str, dict]]:
    """Yields every `(path, period)` pair under every telemetry sensor."""
    for user_id, aquarium_id, telemetry in iterate_telemetry(production_database):
        for sensor_kind, sensor in telemetry.items():
            for period_kind, period in sensor.items():
                if period_kind != "last_instant":
                    yield f"{user_id}/telemetry/{aquarium_id}/{sensor_kind}/{period_kind}", period


def test_database_satisfies_schema(
    production_database: dict,
    database_schema: dict,
) -> None:
    """Tests that the production database satisfies the schema."""
    validate(
        production_database,
        database_schema,
        format_checker=FormatChecker(),
    )


def test_schema_timestamp_window_is_current(
    database_schema: dict,
) -> None:
    """Tests the schema still accepts telemetry recorded right now."""
    timestamp_schema = database_schema["$defs"]["instant"]["properties"]["timestamp"]
    lower_bound, upper_bound = timestamp_schema["minimum"], timestamp_schema["maximum"]
    now = int(time())

    assert lower_bound <= now <= upper_bound,\
    f"Now ({now}) falls outside the accepted timestamp window ({lower_bound}, {upper_bound}); "\
    "widen it in database/schema.json and database/rules.json"


def test_users_have_database_nodes(
    production_database: dict,
    account_uids: set[str],
) -> None:
    """Tests every defined user has a database node."""
    missing_uid_nodes = account_uids - set(production_database)

    assert not missing_uid_nodes, f"Found UIDs without database nodes: {missing_uid_nodes}"


def test_database_nodes_have_users(
    production_database: dict,
    account_uids: set[str],
) -> None:
    """Tests every database node has a defined user."""
    dangling_database_nodes = set(production_database) - account_uids

    assert not dangling_database_nodes,\
    f"Found database nodes without UIDs: {dangling_database_nodes}"


def test_account_emails_match_users(
    production_database: dict,
    auth_users: dict[str, UserRecord],
) -> None:
    """Tests every account node mirrors the email of the user owning it."""
    mismatched_emails: dict[str, tuple] = {}

    for user_id, user_node in production_database.items():
        auth_user = auth_users.get(user_id)
        if auth_user is None:
            continue

        account_email = (user_node.get("account") or {}).get("email")
        if account_email != auth_user.email:
            mismatched_emails[user_id] = (account_email, auth_user.email)

    assert not mismatched_emails,\
    f"Found accounts whose email differs from its user: {mismatched_emails}"


def test_names_and_emails_are_within_rule_limits(
    production_database: dict,
) -> None:
    """Tests every stored name and email satisfies the lengths the database rules enforce."""
    invalid_fields: dict[str, object] = {}

    for user_id, user_node in production_database.items():
        account = user_node.get("account") or {}
        name, email = account.get("name"), account.get("email")

        if not isinstance(name, str) or not 1 <= len(name) <= MAX_ACCOUNT_NAME_LENGTH:
            invalid_fields[f"{user_id}/account/name"] = name
        if not isinstance(email, str) or not 1 <= len(email) <= MAX_ACCOUNT_EMAIL_LENGTH:
            invalid_fields[f"{user_id}/account/email"] = email

    for user_id, aquarium_id, aquarium in iterate_aquariums(production_database):
        name = aquarium.get("name")
        if not isinstance(name, str) or not 1 <= len(name) <= MAX_AQUARIUM_NAME_LENGTH:
            invalid_fields[f"{user_id}/aquariums/{aquarium_id}/name"] = name

    assert not invalid_fields, f"Found names or emails outside the rule limits: {invalid_fields}"


def test_aquariums_are_device_accounts(
    production_database: dict,
    device_uids: set[str],
) -> None:
    """Tests every aquarium ID is the UID of a device account."""
    unbacked_aquariums = set()

    for user_id, aquarium_id, _ in iterate_aquariums(production_database):
        if aquarium_id not in device_uids:
            unbacked_aquariums.add(f"{user_id}/aquariums/{aquarium_id}")

    assert not unbacked_aquariums,\
    f"Found aquariums without a {DEVICE_EMAIL_DOMAIN} account: {unbacked_aquariums}"


def test_telemetry_aquariums_are_registered(
    production_database: dict,
) -> None:
    """Tests every telemetry node belongs to an aquarium registered under the same user."""
    unregistered_telemetry = set()

    for user_id, aquarium_id, _ in iterate_telemetry(production_database):
        aquariums = production_database[user_id].get("aquariums") or {}
        if aquarium_id not in aquariums:
            unregistered_telemetry.add(f"{user_id}/telemetry/{aquarium_id}")

    assert not unregistered_telemetry,\
    f"Found telemetry without a matching aquarium: {unregistered_telemetry}"


def test_thresholds_are_strictly_ordered(
    production_database: dict,
) -> None:
    """Tests every threshold band is ordered warn_low < safe_low < safe_high < warn_high."""
    unordered_thresholds: dict[str, dict] = {}

    for user_id, aquarium_id, aquarium in iterate_aquariums(production_database):
        for sensor_kind, thresholds in (aquarium.get("thresholds") or {}).items():
            bounds = [thresholds.get(key) for key in THRESHOLD_KEYS]
            ordered = all(
                isinstance(low, (int, float)) and isinstance(high, (int, float)) and low < high
                for low, high in zip(bounds, bounds[1:])
            )
            if not ordered:
                path = f"{user_id}/aquariums/{aquarium_id}/thresholds/{sensor_kind}"
                unordered_thresholds[path] = thresholds

    assert not unordered_thresholds,\
    f"Found thresholds that are not strictly increasing: {unordered_thresholds}"


def test_period_indices_reference_existing_buckets(
    production_database: dict,
) -> None:
    """Tests every period index points at a bucket that has actually been committed."""
    dangling_indices: dict[str, object] = {}

    for path, period in iterate_periods(production_database):
        index, buckets = period.get("index"), period.get("buckets") or {}
        if not isinstance(index, int) or bucket_key(index) not in buckets:
            dangling_indices[path] = index

    assert not dangling_indices,\
    f"Found period indices without a committed bucket: {dangling_indices}"


def test_newest_bucket_matches_period_index(
    production_database: dict,
) -> None:
    """
    Tests the bucket a period index points at holds the newest timestamp of that period.

    A device writes the bucket before advancing the index, so a snapshot taken between the
    two writes legitimately sees the following slot as the newest one.
    """
    stale_indices: dict[str, tuple] = {}

    for path, period in iterate_periods(production_database):
        index = period.get("index")
        timestamps = {
            key: bucket["timestamp"]
            for key, bucket in (period.get("buckets") or {}).items()
            if "timestamp" in bucket
        }
        if not isinstance(index, int) or not timestamps:
            continue

        newest_key, _ = max(timestamps.items(), key=lambda item: item[1])
        if newest_key not in (bucket_key(index), bucket_key((index+1) % BUCKET_COUNT)):
            stale_indices[path] = (index, newest_key)

    assert not stale_indices,\
    f"Found periods whose index is not the newest bucket: {stale_indices}"


def test_timestamps_are_not_in_the_future(
    production_database: dict,
) -> None:
    """Tests no telemetry claims to have been recorded later than now."""
    latest_acceptable = int(time()) + CLOCK_SKEW_TOLERANCE_SECS
    future_timestamps: dict[str, int] = {}

    for path, instant in iterate_instants(production_database):
        if instant.get("timestamp", 0) > latest_acceptable:
            future_timestamps[path] = instant["timestamp"]

    for path, period in iterate_periods(production_database):
        for key, bucket in (period.get("buckets") or {}).items():
            if bucket.get("timestamp", 0) > latest_acceptable:
                future_timestamps[f"{path}/buckets/{key}"] = bucket["timestamp"]

    assert not future_timestamps,\
    f"Found timestamps later than {latest_acceptable}: {future_timestamps}"
