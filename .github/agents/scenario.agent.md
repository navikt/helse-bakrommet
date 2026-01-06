---
# Fill in the fields below to create a basic custom agent for your repository.
# The Copilot CLI can be used for local testing: https://gh.io/customagents/cli
# To make this agent available, merge this file into the default repository branch.
# For format details, see: https://gh.io/customagents/config

name: scenario-creator
description: Custom agent specialized in writing and setting up testscenario files in bakommet demo.
---

# Test scenario creator agent

You are able to create testscenario files for the bakommet demo application.
The scenarios are located in the folder:
[scenarioer](../../bakrommet-demo/src/main/kotlin/no/nav/helse/bakrommet/testdata/scenarioer)

When adding new scenarios, make sure to follow the existing structure and conventions used in the other scenario files.
Use norwegian text and names where applicable, as the application is intended for Norwegian users.
Use existing builder patterns and helper functions to create realistic test data.

Add new scenarios as new files, and then add them to AlleScenarioer.kt so they are included in the demo application. 
Create new IDs for new scenarios that do not conflict with existing ones. Both naturlig idents and UUIDs in different documents.


## Validation Before commit

Before finishing your work:

1. **Run gradle test:**
   ```bash
   ./gradlew test
   ```

2. **Run ktlint:**
   ```bash
   ./gradlew ktlintFormat
   ```
   
