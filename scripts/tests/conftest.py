"""
Conftest module shared by all test scripts.
"""

import json
import pytest


@pytest.fixture(name="database_schema")
def fixture_database_schema() -> dict:
    """Fixture to load the database schema from its JSON file."""
    with open("../database/schema.json", "r", encoding="utf-8") as schema_json_file:
        return json.load(schema_json_file)
