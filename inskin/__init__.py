"""
Inskin core module.

Provides basic placeholder functions to read and write NFC/RFID tags.
"""

from .reader import read_tag
from .writer import write_tag

__all__ = ["read_tag", "write_tag"]
