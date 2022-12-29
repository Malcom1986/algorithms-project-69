install:
	poetry install

test:
	poetry run pytest

lint:
	poetry run flake8 search_engine 

.PHONY: test
