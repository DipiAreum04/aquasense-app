"""Provides the `FirebaseDatabase` class."""

import logging
import json
from time import time

import pyrebase as pb


_DEFAULT_LIFETIME_SECS = 3600
_REFRESH_MARGIN_SECS = 300


def _lifetime_secs(creds: dict) -> int:
    """Read how long a sign-in response says its token is good for.

    Google ID tokens last an hour, and `expiresIn` says so on every response, so the
    default here is only ever reached if one comes back malformed.
    """
    try:
        return int(creds["expiresIn"])
    except (KeyError, TypeError, ValueError):
        return _DEFAULT_LIFETIME_SECS


class FirebaseDatabase:
    """Represents a user-aquarium-specific database for convenience."""
    _database: pb.Database
    _auth: pb.Auth
    _user_id: str
    _device_id: str
    _refresh_at: int
    user_token: str
    device_token: str

    def __init__(self) -> None:
        logger = logging.getLogger(__name__)

        with open("ProdDbAppConfig.json", "r", encoding="utf-8") as app_config_json_file:
            firebase = pb.initialize_app(json.load(app_config_json_file))
        self._database = firebase.database()
        self._auth = firebase.auth()

        self.reauthenticate()
        logger.info("Logged in as %s", self._user_id)

        aquariums = self.aquariums_node.get(self.user_token).val() or {}
        if self._device_id not in aquariums.keys():
            self.aquariums_node.child(self._device_id).set({
                "name": "Mock Aquarium",
                "water_type": "freshwater",
            }, self.device_token)
            logger.debug("Mock Aquarium not found, autocreated with id %s", self._device_id)
        else:
            logger.debug("Mock Aquarium found with id %s", self._device_id)

    def ensure_fresh_token(self, current_time: int) -> None:
        """Re-signs in once the tokens are within the refresh margin of expiring.

        A no-op on almost every tick, so the caller can hand it the loop's clock
        unconditionally rather than keeping a refresh schedule of its own.
        """
        if current_time < self._refresh_at:
            return

        self.reauthenticate()

    def reauthenticate(self) -> None:
        """Refreshes login tokens.

        The next refresh is scheduled off the lifetime the sign-in response reports rather
        than a fixed interval, so it tracks whatever Firebase actually issued, and it is
        brought forward by a margin so the token is replaced while it is still good. Both
        tokens are minted here, and a write authorised with either one is rejected once it
        expires, so the schedule follows whichever of the two dies first.

        The margin is clamped to the lifetime it is taken out of, so a token issued for
        less time than the margin schedules a refresh immediately rather than one whose
        deadline has already gone by.
        """
        logger = logging.getLogger(__name__)

        user_creds = self._auth.sign_in_with_email_and_password(
            "test.testington@example.com",
            "TestTest123!",
        )
        self._user_id = user_creds["localId"]
        self.user_token = user_creds["idToken"]
        device_creds = self._auth.sign_in_with_email_and_password(
            "mockdev-0000000001@aquasense.ca",
            "TestTest123!",
        )
        self._device_id = device_creds["localId"]
        self.device_token = device_creds["idToken"]

        lifetime_secs = min(_lifetime_secs(user_creds), _lifetime_secs(device_creds))
        margin_secs = min(lifetime_secs, _REFRESH_MARGIN_SECS)
        self._refresh_at = int(time()) + lifetime_secs - margin_secs

        logger.info("Authenticated user %s with device %s", self._user_id, self._device_id)
        logger.debug(
            "Tokens live for %d seconds, refreshing at %d",
            lifetime_secs,
            self._refresh_at,
        )

    @property
    def aquariums_node(self) -> pb.Database:
        """Returns the user node."""
        return self._database.child(self._user_id).child("aquariums")

    @property
    def telemetry_aquarium_node(self) -> pb.Database:
        """Returns the user node."""
        return self._database.child(self._user_id).child("telemetry").child(self._device_id)

    def read_thresholds(self) -> dict[str, object]:
        """Reads the aquarium's threshold bands, keyed by the sensor each one configures.

        Read with the user token rather than the device one. The bands are the app's to
        edit and live under the owner's aquarium node, which the rules open only to the
        account owning it, while the device account is granted nothing but its own
        telemetry node. A sensor the app has not configured, `water_level` among them, is
        simply absent, as is the whole node until the first band is written.
        """
        thresholds = (self.aquariums_node
            .child(self._device_id)
            .child("thresholds")
            .get(self.user_token).val() or {}
        )

        return dict(thresholds)

    def read_period_indices(
        self, sensor_id: str, period_ids: tuple[str, ...]
    ) -> dict[str, int | None]:
        """Reads one sensor's whole telemetry node and returns every period's bucket index."""
        sensor = (self.telemetry_aquarium_node
            .child(sensor_id)
            .get(self.device_token).val() or {}
        )
        indices = {
            period_id: (sensor.get(period_id) or {}).get("index")
            for period_id in period_ids
        }

        return indices
