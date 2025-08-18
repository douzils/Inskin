import pytest

from inskin import Tag, read_tag, write_tag


def test_read_tag_returns_mock_data():
    tag = read_tag()
    assert isinstance(tag, Tag)
    assert tag.uid == "DEADBEEF"


def test_write_tag_validates_input(capsys):
    payload = {"text": "hello"}
    write_tag(payload)
    captured = capsys.readouterr()
    assert "Writing data to tag: {'text': 'hello'}" in captured.out

    with pytest.raises(TypeError):
        write_tag("not a mapping")
