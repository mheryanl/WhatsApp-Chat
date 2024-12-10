# WhatsApp-Chat
This project demonstrates a server-client group chat application implemented in Java. It allows clients to interact with the server to manage groups, send messages, transfer files, and more.

## Features

- **Add a Group**: Users can create a new group on the server.
- **Join a Group**: Users can join an existing group.
- **Send Messages**: Users can send messages to a group.
- **Send Files**: Users can send files to a group.
- **Leave a Group**: Users can leave a group they joined.
- **Remove a Group**: The server can remove an empty group.

## How to Run

1. Compile the Java files using:
   javac Server.java Client.java
2. Start the server:
   java Server
3. Start the client:
   java Client
4. Use the commands listed in the "Commands" section to interact with the server.

## Commands

AddGroup|<group_name>: Creates a new group on the server.

JoinGroup|<group_name>: Joins an existing group.

SendMessage|<group_name>|<message>: Sends a message to the group.

SendFile|<group_name>|<file_path>: Sends a file to the group.

LeaveGroup|<group_name>: Leaves a joined group.

RemoveGroup|<group_name>: Removes an empty group from the server.

## Example Usage

To create a group: AddGroup|group1
To join a group: JoinGroup|group1
To send a message to a group: SendMessage|group1|Hello everyone!
To send a file to a group: SendFile|group1|path/to/file.txt
To leave a group: LeaveGroup|group1
To remove a group (only if the group is empty): RemoveGroup|group1
