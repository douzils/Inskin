from inskin import read_tag


def test_read_tag_returns_mock_data():
    tag = read_tag()
    assert tag["uid"] == "DEADBEEF"
