from dataclasses import dataclass

@dataclass
class Tag:
    """Simple representation of an NFC/RFID tag."""
    uid: str
    type: str
