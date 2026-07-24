"""
Contains production database validation tests.
"""

import json

from jsonschema import validate, FormatChecker
from firebase_admin.credentials import Certificate
from firebase_admin.auth import list_users
from firebase_admin import db
import firebase_admin as fb
import pytest


@pytest.fixture(name="production_database", scope="session")
def fixture_production_database() -> dict:
    """Fixture that provides the entire production database."""
    with open("ProdDbAppConfig.json", "r", encoding="utf-8") as app_config_json_file:
        app_config_data = json.load(app_config_json_file)

    certificate = Certificate("ProdDbServiceAccountKey.json")
    fb.initialize_app(
        certificate,
        options={ "databaseURL": app_config_data["databaseURL"] },
    )

    admin_db = db.reference()
    database = admin_db.get() or {}
    assert isinstance(database, dict)
    return database


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


def test_users_have_database_nodes(
    production_database: dict,
) -> None:
    """Tests every defined user has a database node."""
    missing_uid_nodes = set()

    page = list_users()
    while page:
        uids = [user.uid for user in page.users]
        for uid in uids:
            if uid not in production_database:
                missing_uid_nodes.add(uid)
        page = page.get_next_page()

    assert not missing_uid_nodes, f"Found UIDs without database nodes: {missing_uid_nodes}"


def test_database_nodes_have_users(
    production_database: dict,
) -> None:
    """Tests every database node has a defined user."""
    dangling_database_nodes = set(production_database.keys())

    page = list_users()
    while page:
        uids = [user.uid for user in page.users]
        for uid in uids:
            if uid in dangling_database_nodes:
                dangling_database_nodes.remove(uid)
        page = page.get_next_page()

    assert not dangling_database_nodes,\
    f"Found database nodes without UIDs: {dangling_database_nodes}"
