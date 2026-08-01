"""Provides the `MockDatabase` and `MockDatabaseNode` classes."""

from typing import Any
from time import sleep
from rstr import xeger


class MockDatabaseNode:
    """Represents a node of the database."""
    _children: dict[str, "MockDatabaseNode"] | Any

    def __init__(self) -> None:
        self._children = None

    def child(self, key: str) -> "MockDatabaseNode":
        """Returns the database node child at the given key."""
        if self._children is None:
            self._children = {}
        return self._children.setdefault(key, MockDatabaseNode())

    def set(self, val: Any) -> None:
        """Sets the value of the database node."""
        sleep(1)
        self._children = val

    def get(self) -> Any:
        """Gets the value of the database node."""
        sleep(1)
        return self._children

    def push(self, val: Any) -> str:
        """Pushes the value into the database node."""
        if self._children is None:
            self._children = {}

        identifier = xeger(r"^[A-Za-z0-9_-]{20}$")
        node = MockDatabaseNode()
        node.set(val)

        self._children[identifier] = node
        return identifier

    def _tree(self, prefix: str = "") -> list[str]:
        lines = []

        if not isinstance(self._children, dict):
            return lines

        items = list(self._children.items())
        for i, (name, child) in enumerate(items):
            last = i == len(items) - 1
            connector = "└── " if last else "├── "

            if isinstance(child, MockDatabaseNode):
                # pylint: disable=protected-access
                if isinstance(child._children, dict):
                    lines.append(prefix + connector + name)
                    extension = "    " if last else "│   "
                    lines.extend(child._tree(prefix + extension))
                else:
                    lines.append(prefix + connector + f"{name}: {child._children!r}")
            else:
                lines.append(prefix + connector + f"{name}: {child!r}")

        return lines

    def __repr__(self) -> str:
        lines = ["AAMS"]
        lines.extend(self._tree())
        return "\n".join(lines)


class MockDatabase(MockDatabaseNode):
    """Mock database."""

    def clear_and_print(self) -> None:
        """Clears the console and prints the database as a tree."""
        print("\033c\033[3J", end="")
        print(f"{self}")
