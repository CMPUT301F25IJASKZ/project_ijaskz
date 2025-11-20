# project_ijaskz

## Product Backlog: https://github.com/orgs/CMPUT301F25IJASKZ/projects/2/views/2

Project Wireframe Figma: https://www.figma.com/design/taNXxhvtTYgmeg5DBcHcog/IJASKZ?node-id=0-1&t=StaJPKUF6zU2Ji2m-1

**New CRC Cards** Found under **/New CRC Cards**

**Object-Oriented Analysis: /Wireframes/ CRC cards**

## UML diagram
![UML](https://www.plantuml.com/plantuml/png/5Ssxhe90483X_Zp5yGKOBx6nWdX9L4XCbCQ6ZCi8YtlPpCvgzEd5wk_-Bu-2BCcQlubsogIM4fl_NYJ45G3ZA-kqzEcM8h7oJiX9fho5TNMgBulfR3TVxW_5-NY5m7uW9OqUC3u-m7IFq1Es4Ing1wKN8Nvld-EX207ggcms6Zj6cuMVdvDhntu1)
Source: [`UML_Diagram.puml`](LotteryEventApp/UML_Diagram.puml)



# Event Lottery System Application

A mobile app for fair and accessible event registration using a lottery system. Entrants join waiting lists, and organizers randomly select participants. Features include QR code event access, Firebase integration, notifications, image uploads, and role-based interactions for entrants, organizers, and admins.

## Features
- Lottery-based selection system
- QR code event access
- Firebase integration
- Notifications for entrants
- Image upload for event posters
- Multi-user roles: entrant, organizer, admin
- Optional geolocation verification

## Roles
**Entrant** – Join or leave event waiting lists, view events, receive lottery notifications, manage profile.  
**Organizer** – Create and manage events, draw participants, send notifications, upload event posters.  
**Admin** – Manage and remove events, profiles, and images.

## Scenario Example
An organizer creates an event with a registration period. Entrants join the waiting list before it closes. When the registration period ends, the system randomly selects participants and notifies them to confirm or decline. Replacements are drawn automatically if spots open.

## Tech
- Android
- Firebase
- QR Code Scanning (Google / ZXing)


