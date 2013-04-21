package swat.js.workers

import swat.js.applications.{NavigatorOnLine, NavigatorLanguage, NavigatorID}

trait WorkerNavigator extends NavigatorID with NavigatorLanguage with NavigatorOnLine
