"""Writer utilities for Inskin.

These functions are placeholders for actual NFC/RFID writer interactions."""

from typing import Any, Mapping


def write_tag(data: Mapping[str, Any]) -> None:
    """Mock writing data to a tag by printing the payload.

    Parameters
    ----------
    data:
        Mapping containing the information to write. Must be a mapping; a
        :class:`TypeError` is raised otherwise.
    """
    if not isinstance(data, Mapping):
        raise TypeError("data must be a mapping")

    print(f"Writing data to tag: {dict(data)}")
