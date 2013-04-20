package swat.api.js.workers

import swat.api.js.applications.{NavigatorOnLine, NavigatorLanguage, NavigatorID}

trait WorkerNavigator extends NavigatorID with NavigatorLanguage with NavigatorOnLine
