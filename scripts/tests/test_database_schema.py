"""
Contains development database unit tests.
"""

import json
from typing import Generator

from requests import HTTPError
from jsonschema import validate, FormatChecker, ValidationError
from firebase_admin.credentials import Certificate
from firebase_admin.auth import get_user_by_email, delete_user, delete_users, list_users
from firebase_admin import db
import firebase_admin as fb
from pyrebase.pyrebase import Auth, Database
import pyrebase as pb
import pytest


@pytest.fixture(name="auth_userdb_admindb_tuple", scope="session")
def fixture_initialize_app() -> tuple[Auth, Database, db.Reference]:
    """
    Fixture to initialize the Firebase app for testing.
    Returns the Firebase Auth and Database instances for use in tests.
    """
    with open("DevDbAppConfig.json", "r", encoding="utf-8") as app_config_json_file:
        app_config_data = json.load(app_config_json_file)

    certificate = Certificate("DevDbServiceAccountKey.json")
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


@pytest.fixture(name="one_test_user_email_pwd", scope="function")
def fixture_setup_one_test_user(
    auth_userdb_admindb_tuple: tuple[Auth, Database, db.Reference],
) -> tuple[str, str]:
    """Fixture to create one test user before each test."""
    auth, user_db, _ = auth_userdb_admindb_tuple

    email, password = "alice@example.com", "password_of_alice"
    creds = auth.create_user_with_email_and_password(email, password)
    user_id, user_token = creds["localId"], creds["idToken"]

    user_db.child(user_id).set(
        { "email": email },
        user_token,
    )

    return email, password


@pytest.fixture(name="teardown_one_test_user", scope="function")
def fixture_teardown_one_test_user(
    auth_userdb_admindb_tuple: tuple[Auth, Database, db.Reference],
) -> Generator[None, None, None]:
    """Fixture to delete one test user after each test."""
    yield
    email = "alice@example.com"
    existing_user = get_user_by_email(email)
    _, _, admin_db = auth_userdb_admindb_tuple
    admin_db.child(existing_user.uid).delete()
    delete_user(existing_user.uid)


@pytest.fixture(name="two_test_users_email_pwd", scope="function")
def fixture_setup_two_test_users(
    auth_userdb_admindb_tuple: tuple[Auth, Database, db.Reference],
) -> tuple[tuple[str, str], tuple[str, str]]:
    """Fixture to create two test users before each test."""
    auth, user_db, _ = auth_userdb_admindb_tuple

    users = (
        ("alice@example.com", "password_of_alice"),
        ("bob@example.com", "password_of_bob"),
    )

    for email, password in users:
        creds = auth.create_user_with_email_and_password(email, password)
        user_id, user_token = creds["localId"], creds["idToken"]

        user_db.child(user_id).set(
            { "email": email },
            user_token,
        )

    return users


@pytest.fixture(name="teardown_two_test_users", scope="function")
def fixture_teardown_two_test_users(
    auth_userdb_admindb_tuple: tuple[Auth, Database, db.Reference],
) -> Generator[None, None, None]:
    """Fixture to delete two test users after each test."""
    yield
    _, _, admin_db = auth_userdb_admindb_tuple
    for email in ["alice@example.com", "bob@example.com"]:
        existing_user = get_user_by_email(email)
        admin_db.child(existing_user.uid).delete()
        delete_user(existing_user.uid)


@pytest.fixture(name="user_with_aquarium", scope="function")
def fixture_create_user_with_aquarium( # pylint: disable=unused-argument
    auth_userdb_admindb_tuple: tuple[Auth, Database, db.Reference],
    one_test_user_email_pwd: tuple[str, str],
    teardown_database: None,
    teardown_one_test_user: Generator[None, None, None],
) -> tuple[str, str, str]:
    """Fixture that creates one test user and adds an aquarium to their account."""
    auth, user_db, _ = auth_userdb_admindb_tuple
    email, password = one_test_user_email_pwd

    user_creds = auth.sign_in_with_email_and_password(email, password)
    user_id, user_token = user_creds["localId"], user_creds["idToken"]

    aquarium_name = "Alice's Aquarium"
    user_db.child(user_id).child("aquariums").push(aquarium_name, user_token)
    return email, password, aquarium_name


def test_empty_database_is_valid( # pylint: disable=unused-argument
    auth_userdb_admindb_tuple: tuple[Auth, Database, db.Reference],
    database_schema: dict,
    teardown_database: None,
) -> None:
    """Test that an empty database is valid against the database schema."""
    _, _, admin_db = auth_userdb_admindb_tuple
    response = admin_db.get() or {}
    validate(response, database_schema, format_checker=FormatChecker())


def test_new_user_with_email_is_valid( # pylint: disable=unused-argument
    auth_userdb_admindb_tuple: tuple[Auth, Database, db.Reference],
    one_test_user_email_pwd: tuple[str, str],
    database_schema: dict,
    teardown_database: None,
    teardown_one_test_user: Generator[None, None, None],
) -> None:
    """Test that a new user with an email is valid against the database schema."""
    auth, _, admin_db = auth_userdb_admindb_tuple
    email, password = one_test_user_email_pwd

    user_creds = auth.sign_in_with_email_and_password(email, password)
    user_id = user_creds["localId"]

    response = admin_db.get()
    assert user_id in response
    validate(response, database_schema, format_checker=FormatChecker())


def test_new_user_without_email_is_invalid( # pylint: disable=unused-argument
    auth_userdb_admindb_tuple: tuple[Auth, Database, db.Reference],
    one_test_user_email_pwd: tuple[str, str],
    database_schema: dict,
    teardown_database: None,
    teardown_one_test_user: Generator[None, None, None],
) -> None:
    """Test that a new user without an email is invalid against the database schema."""
    auth, _, admin_db = auth_userdb_admindb_tuple
    email, password = one_test_user_email_pwd

    user_creds = auth.sign_in_with_email_and_password(email, password)
    user_id = user_creds["localId"]

    admin_db.child(user_id).child("aquariums").push("KEEP_ALIVE")
    admin_db.child(user_id).child("email").delete()

    response = admin_db.get()
    assert user_id in response
    with pytest.raises(ValidationError):
        validate(response, database_schema, format_checker=FormatChecker())


def test_new_user_with_additional_properties_is_invalid( # pylint: disable=unused-argument
    auth_userdb_admindb_tuple: tuple[Auth, Database, db.Reference],
    one_test_user_email_pwd: tuple[str, str],
    database_schema: dict,
    teardown_database: None,
    teardown_one_test_user: Generator[None, None, None],
) -> None:
    """Test that a new user with additional properties is invalid against the database schema."""
    auth, _, admin_db = auth_userdb_admindb_tuple
    email, password = one_test_user_email_pwd

    user_creds = auth.sign_in_with_email_and_password(email, password)
    user_id = user_creds["localId"]

    admin_db.child(user_id).child("additional_property").set(True)

    response = admin_db.get()
    assert user_id in response
    with pytest.raises(ValidationError):
        validate(response, database_schema, format_checker=FormatChecker())


def test_user_can_access_self( # pylint: disable=unused-argument
    auth_userdb_admindb_tuple: tuple[Auth, Database, db.Reference],
    one_test_user_email_pwd: tuple[str, str],
    teardown_database: None,
    teardown_one_test_user: Generator[None, None, None],
) -> None:
    """Test that a user can access their database node."""
    auth, user_db, _ = auth_userdb_admindb_tuple
    email, password = one_test_user_email_pwd

    user_creds = auth.sign_in_with_email_and_password(email, password)
    user_id, user_token = user_creds["localId"], user_creds["idToken"]

    assert user_db.child(user_id).get(user_token).val()


def test_user_cannot_access_other( # pylint: disable=unused-argument
    auth_userdb_admindb_tuple: tuple[Auth, Database, db.Reference],
    two_test_users_email_pwd: tuple[tuple[str, str], tuple[str, str]],
    teardown_database: None,
    teardown_two_test_users: Generator[None, None, None],
) -> None:
    """Test that a user cannot access another's database node."""
    auth, user_db, _ = auth_userdb_admindb_tuple
    alice, bob = two_test_users_email_pwd

    alice_email, alice_password = alice
    bob_email, bob_password = bob

    alice_creds = auth.sign_in_with_email_and_password(alice_email, alice_password)
    alice_id, _ = alice_creds["localId"], alice_creds["idToken"]
    bob_creds = auth.sign_in_with_email_and_password(bob_email, bob_password)
    _, bob_token = bob_creds["localId"], bob_creds["idToken"]

    with pytest.raises(HTTPError):
        assert user_db.child(alice_id).get(bob_token).val()


def test_user_add_aquarium(
    auth_userdb_admindb_tuple: tuple[Auth, Database, db.Reference],
    user_with_aquarium: tuple[str, str, str],
    database_schema: dict,
) -> None:
    """Test that a user can add an aquarium and the data is valid against the database schema."""
    auth, user_db, admin_db = auth_userdb_admindb_tuple
    email, password, aquarium_name = user_with_aquarium

    user_creds = auth.sign_in_with_email_and_password(email, password)
    user_id, user_token = user_creds["localId"], user_creds["idToken"]

    aquariums = user_db.child(user_id).child("aquariums").get(user_token).val()
    assert isinstance(aquariums, dict)
    aquarium_id = list(aquariums.keys()).pop()

    response = admin_db.get()
    assert user_id in response
    assert aquarium_id in response[user_id]["aquariums"]
    assert response[user_id]["aquariums"][aquarium_id] == aquarium_name
    validate(response, database_schema, format_checker=FormatChecker())
