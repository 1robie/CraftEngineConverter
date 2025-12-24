# Security Policy

## Supported Versions

Only the latest major and minor versions of CraftEngineConverter are actively supported with security updates. Please update to the latest release to ensure you receive important security patches.

| Version | Supported          |
| ------- | ----------------- |
| latest  | :white_check_mark:|
| <latest | :x:               |

## Reporting a Vulnerability

If you discover a security vulnerability in this project, please report it responsibly:

1. **Do not disclose vulnerabilities publicly** until they have been reviewed and patched.
2. **For critical or severe vulnerabilities, do NOT open a public issue.** Instead, contact the maintainer directly and privately on Discord: `kyubi8319`. Please send a clear and detailed message, or your message may be ignored or lost in spam.
3. For less severe issues, you may use [GitHub Issues](https://github.com/1robie/CraftEngineConverter/issues) and mark the issue as confidential or security-related, but private contact is preferred for anything sensitive.
4. Provide as much detail as possible, including steps to reproduce, potential impact, and suggested mitigations.

## Responsible Disclosure

We appreciate responsible disclosure of security issues. We will respond as quickly as possible and work with you to resolve the issue. Please allow a reasonable time for us to address the vulnerability before any public disclosure.

## Security Best Practices

- **Do not run the plugin with unnecessary privileges.**
- **Keep your server and dependencies up to date.**
- **Never use untrusted or modified versions of the plugin.**
- **Review configuration files for sensitive data before sharing.**
- **Be cautious when extracting or handling user-supplied files (e.g., ZIPs) to avoid directory traversal and similar attacks.**

## Security Features

- Input validation and sanitization for file operations (including ZIP extraction).
- Protection against directory traversal attacks.
- Regular dependency vulnerability checks.

## Acknowledgements

If you find a security issue, thank you for helping keep the CraftEngineConverter community safe!
