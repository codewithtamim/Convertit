# Contributing

Thank you for contributing to the ConvertIt project. As a contributor, here are the guidelines we would like you to follow:

* [Contributing Code](#contributing-code)
* [Creating an Issue](#creating-an-issue)
* [Pull Requests](#pull-requests)
* [Git Workflow](#git-workflow)
* [Commit Message Guidelines](#commit-message-guidelines)

<a name="contributing-code"></a>
## Contributing Code

* By contributing to this project, you agree to the terms stated in the Contributor License Agreement.
* By contributing to this project, you share your code under the GNU General Public License v3 (GPLv3), as specified in the [LICENSE](LICENSE) file.
* Do not forget to add yourself to the Authors file if applicable.

<a name="creating-an-issue"></a>
## Creating an Issue

* If you want to report a security problem, do not create an issue. Instead, contact the maintainers through appropriate channels.
* When creating a new issue, choose a Bug report or Feature request template and fill in the required information.
* Please describe the steps necessary to reproduce the issue you are running into.
* Include relevant environment details such as Android version, device model, and ConvertIt version.

<a name="pull-requests"></a>
## Pull Requests

Good pull requests, such as patches, improvements, and new features, are a fantastic help. They should remain focused in scope and avoid containing unrelated commits.

Please ask first before embarking on any significant pull request, such as implementing features or refactoring code, otherwise you risk spending a lot of time working on something that the developers might not want to merge into the project.

Follow these steps when you want to submit a pull request:

1. Review the installation and setup guides in the [README](README.md)
2. Follow all instructions in the PR template if available
3. Update the [README](README.md) file with details of changes if applicable
4. Test your changes thoroughly before submitting

<a name="git-workflow"></a>
## Git Workflow

This project uses a standard Git workflow.

### Branch Naming Guidelines

Naming for branches is made with the following structure:

```
<type>/<issue-id>-<short-summary-or-description>
```

In case when there is no issue:

```
<type>/<short-summary-or-description>
```

Where <type> can be `feature`, `fix`, `task`, `bugfix`, or `refactor`.

### Branches

* `main` - The production branch. Clone or fork this repository for the latest copy.
* `develop` - The active development branch. Pull requests should be directed to this branch.
* `<feature branch>` - The feature or fix branch. Pull requests should be made from this branch into `develop` branch.

### Getting Started

This repository uses `.githooks` as `core.hooksPath` so commit messages stay clean. After cloning, run:

```bash
git config core.hooksPath .githooks
```

1. Fork and clone the repository:

```bash
git clone https://github.com/your-username/Convertit.git
```

2. Create a feature or fix branch:

```bash
git checkout -b feature/your-feature-name
```

<a name="commit-message-guidelines"></a>
## Commit Message Guidelines

We have very precise rules over how our git commit messages should be formatted. This leads to readable messages that are easy to follow when looking through the project history.

### Commit Message Format

We follow the Conventional Commits specification. A commit message consists of a **header**, **body**, and **footer**. The header has a **type**, **scope**, and **subject**:

```
<type>(<scope>): <subject>

<body>

<footer>
```

The header is mandatory and the scope of the header is optional.

### Type

Must be one of the following:

* **feat**: A new feature
* **fix**: A bug fix
* **docs**: Documentation only changes
* **style**: Changes that do not affect the meaning of the code, such as white-space, formatting, or missing semi-colons
* **refactor**: A code change that neither fixes a bug nor adds a feature
* **perf**: A code change that improves performance
* **test**: Adding missing tests
* **build**: Changes that affect the build system
* **ci**: Changes to CI configuration files and scripts
* **chore**: Changes to the build process or auxiliary tools and libraries such as documentation generation

### Examples

```
feat(audio): add support for OPUS codec
```
```
fix(converter): resolve file permission issue on Android 13
```
```
docs(readme): update installation instructions
```
```
refactor(ui): simplify conversion flow logic
```

### Writing the Body

Use the body to explain what and why rather than how. Wrap text at 72 characters.

### Writing the Footer

The footer should contain any information about Breaking Changes or issue references.

Breaking changes should start with `BREAKING CHANGE:` followed by a description.

```
BREAKING CHANGE: removed deprecated convertFile method
```

---

Thank you for helping make ConvertIt better!