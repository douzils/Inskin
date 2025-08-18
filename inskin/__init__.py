"""
Inskin core module.

Provides basic placeholder functions to read and write NFC/RFID tags.
"""

"""Public API for the :mod:`inskin` package."""

from .tag import Tag
from .reader import read_tag
from .writer import write_tag

__all__ = ["Tag", "read_tag", "write_tag"]
