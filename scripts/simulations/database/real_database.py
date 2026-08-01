"""Provides the `RealDatabase` class."""

import logging
import json
import pyrebase as pb


class RealDatabase:
    """Represents a user-aquarium-specific database for convenience."""
    _database: pb.Database
    _auth: pb.Auth
    _user_id: str
    _device_id: str

    def __init__(self) -> None:
        logger = logging.getLogger(__name__)

        with open("ProdDbAppConfig.json", "r", encoding="utf-8") as app_config_json_file:
            firebase = pb.initialize_app(json.load(app_config_json_file))
        self._database = firebase.database()
        self._auth = firebase.auth()

        self.reauthenticate()
        logger.info("Logged in as %s", self._user_id)

        aquariums = self.aquariums_node.get().val() or {}
        if self._device_id not in aquariums.keys():
            self.aquariums_node.child(self._device_id).set({
                "name": "Mock Aquarium",
                "water_type": "freshwater",
            })
            logger.debug("Mock Aquarium not found, autocreated with id %s", self._device_id)
        else:
            logger.debug("Mock Aquarium found with id %s", self._device_id)

    def reauthenticate(self) -> None:
        """Refreshes login tokens."""
        logger = logging.getLogger(__name__)

        self._user_id = self._auth.sign_in_with_email_and_password(
            "test.testington@example.com",
            "TestTest123!",
        )["localId"]
        self._device_id = self._auth.sign_in_with_email_and_password(
            "mockdev-0000000001@aquasense.ca",
            "TestTest123!",
        )["localId"]
        logger.info("Authenticated user %s with device %s", self._user_id, self._device_id)

    @property
    def aquariums_node(self) -> pb.Database:
        """Returns the user node."""
        return self._database.child(self._user_id).child("aquariums")

    @property
    def telemetry_aquarium_node(self) -> pb.Database:
        """Returns the user node."""
        return self._database.child(self._user_id).child("telemetry").child(self._device_id)
