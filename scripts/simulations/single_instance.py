"""Provides `claim_single_instance`.

The lock file sits in the machine's temporary directory rather than beside the script, so a
second checkout of the repository is held off too. Two clones running at once are two writers on
one database exactly like two runs of one clone, and the board they both claim to be does not care
which directory they were started from.

The claim is staked on byte zero alone, and the holder's process id written from byte one onwards,
because a Windows lock is mandatory rather than advisory: no other process can read a byte this
one holds. Keeping the id outside the locked byte is what lets the instance that loses the race
name the one that won, which is the whole point of writing it down.

The winning handle is kept at module level, because the claim lasts exactly as long as that handle
does: closing it, including having it closed by the garbage collector because nothing referred to
it any more, hands the lock back while the simulator is still running. That reference keeps it
alive for the life of the process, which is also why nothing here releases the lock. The
only ways out of the tick loop are a keyboard interrupt and being killed, and the operating system
drops the lock on both. A lock dropped by the kernel cannot go stale, so nothing has to recover
one that did.
"""

import logging
import os
import sys
import tempfile
from pathlib import Path
from typing import BinaryIO
import msvcrt

_LOCK_PATH = Path(tempfile.gettempdir()) / "aquasense-simulator.lock"
_OWNER_OFFSET = 1

_HOLDER: list[BinaryIO] = []


def _try_claim(handle: BinaryIO) -> bool:
    """Take the lock on the claim byte, reporting whether it was free."""
    handle.seek(0)
    try:
        msvcrt.locking(handle.fileno(), msvcrt.LK_NBLCK, 1)
    except OSError:
        return False

    return True


def claim_single_instance() -> None:
    """Claim the sole right to run as the simulator, exiting if another instance holds it.

    The simulator is the board as far as the database is concerned, and a board is one device.
    Two runs at once are two independent sets of sensors, each with its own smoothed values, its
    own spike stage and its own test schedule, writing a second apart to the one `last_instant`
    each sensor has. The app reads that node the only way there is to read it, so a card flips
    between the two of them: a spike test reads as a value flickering back and forth twice a
    second, and an offline test as a sensor dropping out and returning between every reading.
    The histories fare worse, both runs committing buckets to the same period nodes and advancing
    their indices independently, so the graphs interleave two sensors that were never one.

    Enforced with a lock the operating system owns rather than a file the simulator writes and
    checks, because the two are not the same guarantee. A written marker has to be cleaned up, so
    a run that is killed leaves one behind and blocks every run after it; and between reading it
    and writing it there is a window where two starts both find it absent. The kernel drops this
    one however the process dies, and hands it to exactly one of any number of simultaneous
    claimants.

    The lock file is opened without truncating it, because it is shared with whoever may already
    hold it: it is there to be written in place, never emptied out from under them.
    """
    logger = logging.getLogger(__name__)

    if _HOLDER:
        return

    handle = os.fdopen(os.open(_LOCK_PATH, os.O_RDWR | os.O_CREAT), "r+b")

    if not _try_claim(handle):
        owner = _read_owner(handle)
        handle.close()

        logger.error(
            "Another simulator instance is already running (process %s). "
            "Stop it before starting this one; two instances write conflicting readings "
            "to the same aquarium.",
            owner,
        )
        sys.exit(1)

    _HOLDER.append(handle)
    _write_owner(handle)

    logger.info("Claimed the simulator lock at %s as process %d", _LOCK_PATH, os.getpid())


def _read_owner(handle: BinaryIO) -> str:
    """Return the process id recorded by the instance holding the claim.

    An instance that has taken the lock but not yet recorded its id leaves nothing to read, and a
    claim held by nobody nameable is still a claim held.
    """
    try:
        handle.seek(_OWNER_OFFSET)
        owner = handle.read().decode(errors="replace").strip()
    except OSError:
        return "unknown"

    return owner or "unknown"


def _write_owner(handle: BinaryIO) -> None:
    """Record this process id for the next instance that finds the claim taken."""
    handle.seek(_OWNER_OFFSET)
    handle.write(str(os.getpid()).encode())
    handle.truncate()
    handle.flush()
