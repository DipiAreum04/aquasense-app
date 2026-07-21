import uuid
import json
import jsonschema
import pytest

from typing import Generator
from firebase_admin.credentials import Certificate
from firebase_admin.auth import get_user_by_email, delete_user, delete_users, list_users
from firebase_admin import db
import firebase_admin as fb
from pyrebase.pyrebase import Auth, Database
import pyrebase as pb


@pytest.fixture(name="auth_userdb_admindb_tuple", scope="session")
def fixture_initialize_app() -> tuple[Auth, Database, db.Reference]:
    """
    Fixture to initialize the Firebase app for testing.
    Returns the Firebase Auth and Database instances for use in tests.
    """
    with open("appConfig.json", "r", encoding="utf-8") as app_config_json_file:
        app_config_data = json.load(app_config_json_file)

    certificate = Certificate("serviceAccountKey.json")
    fb.initialize_app(
        certificate,
        options={ "databaseURL": app_config_data["databaseURL"] },
    )

    page = list_users()
    while page:
        uids = [user.uid for user in page.users]
        if uids:
            delete_users(uids)
        page = page.get_next_page()

    admin_db = db.reference()
    admin_db.delete()

    firebase = pb.initialize_app(app_config_data)
    return firebase.auth(), firebase.database(), admin_db


@pytest.fixture(name="teardown_database", scope="function")
def fixture_teardown_database() -> Generator[None, None, None]:
    """Fixture to teardown the database after each test."""
    yield
    db.reference().delete()


@pytest.fixture(name="database_schema")
def fixture_database_schema() -> dict:
    """Fixture to load the database schema from its JSON file."""
    with open("../database/schema.json", "r", encoding="utf-8") as schema_json_file:
        return json.load(schema_json_file)


@pytest.fixture(name="one_test_user_email_pwd", scope="function")
def fixture_setup_one_test_user(
    auth_userdb_admindb_tuple: tuple[Auth, Database, db.Reference],
) -> tuple[str, str]:
    """Fixture to create one test user before each test."""
    auth, _, _ = auth_userdb_admindb_tuple

    email, password = "alice@example.com", "password_of_alice"
    auth.create_user_with_email_and_password(email, password)
    return email, password


@pytest.fixture(name="teardown_one_test_user", scope="function")
def fixture_teardown_one_test_user(
    auth_userdb_admindb_tuple: tuple[Auth, Database, db.Reference],
) -> Generator[None, None, None]:
    """Fixture to delete one test user after each test."""
    yield
    email = "alice@example.com"
    existing_user = get_user_by_email(email)
    delete_user(existing_user.uid)


@pytest.fixture(name="two_test_users_email_pwd", scope="function")
def fixture_setup_two_test_users(
    auth_userdb_admindb_tuple: tuple[Auth, Database, db.Reference],
) -> tuple[str, str]:
    """Fixture to create two test users before each test."""
    auth, _, _ = auth_userdb_admindb_tuple

    users = (
        ("alice@example.com", "password_of_alice"),
        ("bob@example.com", "password_of_bob"),
    )

    for email, password in users:
        auth.create_user_with_email_and_password(email, password)

    return users


@pytest.fixture(name="teardown_two_test_users", scope="function")
def fixture_teardown_two_test_users(
    auth_userdb_admindb_tuple: tuple[Auth, Database, db.Reference],
) -> Generator[None, None, None]:
    """Fixture to delete two test users after each test."""
    yield
    for email in ["alice@example.com", "bob@example.com"]:
        existing_user = get_user_by_email(email)
        delete_user(existing_user.uid)


def test_empty_database_is_valid(
    auth_userdb_admindb_tuple: tuple[Auth, Database, db.Reference],
    database_schema: dict,
    teardown_database: None,
) -> None:
    """Test that an empty database is valid against the database schema."""
    _, _, admin_db = auth_userdb_admindb_tuple
    response = admin_db.get() or {}
    jsonschema.validate(response, database_schema, format_checker=jsonschema.FormatChecker())


def test_new_user_with_email_is_valid(
    auth_userdb_admindb_tuple: tuple[Auth, Database, db.Reference],
    one_test_user_email_pwd: tuple[str, str],
    database_schema: dict,
    teardown_database: None,
    teardown_one_test_user: Generator[None, None, None],
) -> None:
    """Test that a new user with an email is valid against the database schema."""
    auth, user_db, admin_db = auth_userdb_admindb_tuple
    email, password = one_test_user_email_pwd

    user_creds = auth.sign_in_with_email_and_password(email, password)
    user_id, user_token = user_creds["localId"], user_creds["idToken"]

    user_db.child("users").child(user_id).set(
        {
            "email": email,
        },
        user_token
    )

    response = admin_db.get()
    assert user_id in response["users"]
    jsonschema.validate(response, database_schema, format_checker=jsonschema.FormatChecker())


def test_new_user_without_email_is_invalid(
    auth_userdb_admindb_tuple: tuple[Auth, Database, db.Reference],
    one_test_user_email_pwd: tuple[str, str],
    database_schema: dict,
    teardown_database: None,
    teardown_one_test_user: Generator[None, None, None],
) -> None:
    """Test that a new user without an email is invalid against the database schema."""
    auth, user_db, admin_db = auth_userdb_admindb_tuple
    email, password = one_test_user_email_pwd

    user_creds = auth.sign_in_with_email_and_password(email, password)
    user_id, user_token = user_creds["localId"], user_creds["idToken"]

    user_db.child("users").child(user_id).set(
        {
            "not_email": email,
        },
        user_token
    )

    response = admin_db.get()
    assert user_id in response["users"]
    with pytest.raises(jsonschema.ValidationError):
        jsonschema.validate(response, database_schema, format_checker=jsonschema.FormatChecker())


def test_new_user_with_additional_properties_is_invalid(
    auth_userdb_admindb_tuple: tuple[Auth, Database, db.Reference],
    one_test_user_email_pwd: tuple[str, str],
    database_schema: dict,
    teardown_database: None,
    teardown_one_test_user: Generator[None, None, None],
) -> None:
    """Test that a new user with additional properties is invalid against the database schema."""
    auth, user_db, admin_db = auth_userdb_admindb_tuple
    email, password = one_test_user_email_pwd

    user_creds = auth.sign_in_with_email_and_password(email, password)
    user_id, user_token = user_creds["localId"], user_creds["idToken"]

    user_db.child("users").child(user_id).set(
        {
            "email": email,
            "additional_property": True,
        },
        user_token
    )

    response = admin_db.get()
    assert user_id in response["users"]
    with pytest.raises(jsonschema.ValidationError):
        jsonschema.validate(response, database_schema, format_checker=jsonschema.FormatChecker())


def test_user_add_aquarium_is_valid(
    auth_userdb_admindb_tuple: tuple[Auth, Database, db.Reference],
    one_test_user_email_pwd: tuple[str, str],
    database_schema: dict,
    teardown_database: None,
    teardown_one_test_user: Generator[None, None, None],
) -> None:
    """Test that a user can add an aquarium and the data is valid against the database schema."""
    auth, user_db, admin_db = auth_userdb_admindb_tuple
    email, password = one_test_user_email_pwd

    user_creds = auth.sign_in_with_email_and_password(email, password)
    user_id, user_token = user_creds["localId"], user_creds["idToken"]

    user_db.child("users").child(user_id).set(
        {
            "email": email,
        },
        user_token
    )

    aquarium_name = "Alice's Aquarium"
    aquarium_id = str(
        uuid.uuid5(uuid.NAMESPACE_URL, f"https://aquasense.com/aquariums/{user_id}/{aquarium_name}")
    )

    aquariums_accessible = (
        user_db.child("users")
            .child(user_id)
            .child("aquariums")
            .get(user_token)
            .val()
    )

    if aquariums_accessible is None:
        aquariums_accessible = []
    aquariums_accessible.append(aquarium_id)

    user_db.child("users").child(user_id).child("aquariums").set(aquariums_accessible, user_token)

    aquarium_data = (
        user_db.child("aquariums")
            .child(aquarium_id)
            .get(user_token)
            .val()
    )

    if aquarium_data is None:
        aquarium_data = {}
    aquarium_data.setdefault("users", {})[user_id] = "o"
    aquarium_data["name"] = aquarium_name

    user_db.child("aquariums").child(aquarium_id).set(aquarium_data, user_token)

    response = admin_db.get()
    assert user_id in response["users"]
    assert aquarium_id in response["users"][user_id]["aquariums"]
    assert aquarium_id in response["aquariums"]
    assert user_id in response["aquariums"][aquarium_id]["users"]
    assert response["aquariums"][aquarium_id]["users"][user_id] == "o"
    jsonschema.validate(response, database_schema, format_checker=jsonschema.FormatChecker())
