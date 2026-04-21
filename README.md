AcademicPro Student Registry is a JavaFX app for managing student records.

HOW TO RUN:
- Open the project in IntelliJ
- Open the pom.xml file and clock Load Maven Changes (The little M icon in the top right)
- Update the database credentials in ```DbConnectivityClass.java``` If you have an Azure sever set up
  ```
  SQL_SERVER_URL = "jdbc:mysql://your-server.mariadb.database.azure.com"
   USERNAME       = "your_username"
   PASSWORD       = "your_password"
  ```
- If you don't then the application will default to a local derby server that runs on your machine

FEATURES:
- Add, edit, and delete student records
- Form validation with real-time green/red field feedback
- Major selection via type-safe enum ComboBox
- Real-time search filtering across all fields
- CSV import and export
- PDF report generation by major (Extra Credit)
- Inline table row editing (Extra Credit)
- Light and dark theme switcher
- Animated status bar messages
- Confirmation dialog before deleting records
- User sign-up and login with credential persistence
- Azure MySQL primary database with Derby embedded feedback
- Thread safe UserSession singleton

MY TOUCH:
1) Academic Pro Branding and Visual Identity
   - Needed because the application had no consistent visual identity and lacked cohesive design.
     A professional app needs a brand that builds trust and makes it recognizable.
   - This was implemented by renaming the application to AcademicPro Student Registry and given a full identiy.
   - I used a primary blue and gold accent as a color pallete and applied it across every screen
   - Button classes were seperated into ```btn_primary```, ```btn_secondary```, and ```btn_danger``` to give visual meanings to actions
2) Real Time Seach and Filtering
   - Needed because the origional application had no way to find a specific student without scrolling though the entire table.
   - ```Filteredlist<Person>``` wraps the underlying ```ObservableList<Person>```, and a SortedList on top of that keeps
     column sorting working while the search is active.
   - A Search feild sits above the table, it's textProperty listener updates
     the filter predicate on every keystroke.
   - ```recordCountLabel``` updates in real time to show how many records are currently
     visible
3) Confirmation Dialog Before Deleting Records
   - The original delte button immediatley removed a record with no warning. If a user does this on accident this can permenantly
     destroy data. Any application that has irreversible operations like this should confirm the users intent first
   - Implemented by adding a JavaFX ```Alert``` of type ```CONFIRMATION``` that is shown every time the delete button
     or menu itemis triggered
4) Animated Status Bat with operation Fedback
   - The origional application gave the user zero feedback after performing actions. THis means records were added edited, or deleted silently.
     This means users have no way of knowing if an operation siceeded or failed. Every modern app should communicate the result
     of an operation clearly
   - Implemented a persistent status bar at the bottom of the main screen containing ```statusLabal``` and ```dbModeLabel```.
   - ```showStatus(message, isError)``` method sets the label text and color, then schedules ```FateTransition``` so that the
     message fades out after 3.5 sec and resets the label to it's ready state.
   - ```dbModeLabel``` also permanently shows whether the app is connected to AzureDB or running on a local Derby

PDF VIEWER EXTRA CREDIT

- A pdf Viewer was implemented useing the iText5 library (```com.itextpdf:itextpdf:5.5.13.3```) as a Maven dependency.
- iText is an unnamed module on the classpath so it canot be directly imported without breaking the module system.
- This was solved by accessing classes entirly though Java reflection in ```DbConnectivityClass.generatePDFReport()```.
- The ```pom.xml``` includes ```--add-reads com.example.csc311_bd_ui-semesterlongproject=ALL-UNNAMED``` so the app
  module can read classpath libraries at runtime.
- The report includes a title, generation date, a summary, and a student listing.
- It's triggered by going to Data -> Gnerate PDF Report...
- This opens a ```File chooser``` save dialogue so the user can name and place the file

INLINE TABLE ROW EDITING EXTRA CREDIT
- Double Clock any cell in the First name, last name, email, or department colums to edit it directly
- ```tv.setEditable(true)``` enables edit mode on ```TableView```.
- ```TextFeildTableCell.forTableColumn()``` is applied as the cell facoty for each editabe collumn
- Every collumns ```setOnEditCommit``` handler calidates the new value against the same regext patterns used by the form
- If validation passes the ```Person``` object is updated and ```cnUtil.editUser()``` persusts. A status bat message cornfirms the result.
