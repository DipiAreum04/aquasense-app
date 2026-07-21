# Miscellaneous Scripts

Directory containing Python scripts mostly used for validation.

## Requirements

[Python 3.14](https://www.python.org/downloads/) must be installed first. The `uv` package manager must be installed using:

```sh
powershell -ExecutionPolicy ByPass -c "irm https://astral.sh/uv/install.ps1 | iex"
```

After changing the current directory to `aquasense/scripts`, all dependencies can be installed using:

```sh
uv sync
```

After this, the terminal should be closed and reopened.

## Usage

To run all validation tests:

```sh
pytest -vv
```

To run specific validation tests:

```sh
pytest -vv tests/<TEST_FILE_NAME>
```
