"""Reader utilities for Inskin.

These functions are placeholders for actual NFC/RFID reader interactions."""

from .tag import Tag


def read_tag() -> Tag:
    """Return mock data representing a scanned :class:`Tag`."""
    return Tag(uid="DEADBEEF", type="placeholder")
