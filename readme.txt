Scenario
    Providing a list of the Jira tasks
        JQL:
            labels NOT IN (WordPress,LocationLanding,LocationPicker,LandingBuild)
            AND issuetype in (Epic, LandingAG)
            AND status = QA
            AND assignee not in (membersOF(QA))";
    Ensure to avoid process the tasks which already have a comment "Build settings done by automation."
    Providing data
        Spot ID
        Test website URL
    Setting fields on Website section
        ADA to true
        Real Website to true
        Wcache to true
        Wcache test location to true
        Start Build button change to Build Finished state and trigger websiteInProgress() function
        Enter test website number
    Invalidating wcache for the Test website
