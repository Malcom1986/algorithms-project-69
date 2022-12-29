from search_engine.search_engine import solution
from pathlib import Path


def get_fixture_path(filename):
    fixtures_path = 'tests/fixtures'
    return Path(fixtures_path, filename).absolute()


def get_document_content(id):
    path = get_fixture_path(id)
    with path.open('r') as doc:
        text = doc.read()
    return {'id': id, 'text': text}


def test_empty():
    result = solution([], '')
    assert len(result) == 0


def test_simple_search():
    search_text = 'trash island'
    doc_ids = [
        'garbage_patch_NG', 'garbage_patch_ocean_clean', 'garbage_patch_wiki'
    ]
    docs = [
        get_document_content(doc_id)
        for doc_id
        in doc_ids
    ]
    result = solution(docs, search_text)
    assert result == doc_ids


def test_with_spam():
    search_text = 'the trash island is a'
    doc_ids = [
        'garbage_patch_NG',
        'garbage_patch_ocean_clean',
        'garbage_patch_wiki',
        'garbage_patch_spam'
    ]
    docs = [
        get_document_content(doc_id)
        for doc_id
        in doc_ids
    ]
    result = solution(docs, search_text)
    assert result == doc_ids


def test_search_with_short_text():
    search_text = 'shoot at me, nerd'
    doc1 = "I can't shoot straight unless I've had a pint!"
    doc2 = "Don't shoot shoot shoot that thing at me."
    doc3 = "I'm your shooter."
    docs = [
        {'id': 'doc1', 'text': doc1},
        {'id': 'doc2', 'text': doc2},
        {'id': 'doc3', 'text': doc3},
    ]
    expected = ['doc2', 'doc1']
    result = solution(docs, search_text)
    assert result == expected
