"""Command-line interface for the Inskin placeholder application."""

from inskin import read_tag, write_tag


def main():
    """Demonstrate reading and writing tag operations."""
    tag = read_tag()
    print("Read tag:", tag)
    write_tag({"text": "Hello from Inskin"})


if __name__ == "__main__":
    main()
