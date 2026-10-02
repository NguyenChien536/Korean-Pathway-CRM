# Korean-Pathway-CRM

Korean-Pathway-CRM is a web-based Customer Relationship Management (CRM) platform specifically designed for study abroad consulting centers, with a focus on Korean education pathways. The system streamlines lead capture, omnichannel messaging, document checklist tracking, and pipeline management.

## Architecture

The system is built with a modern, layered architecture:

- **Frontend:** React 18, Vite, Redux Toolkit, Ant Design, Tailwind CSS
- **Backend:** Java 17+, Spring Boot 3.x, Spring Security (JWT), WebSocket (STOMP)
- **Database:** PostgreSQL (utilizing JSONB for webhook payloads)
- **Infrastructure:** Docker, AWS S3 / Cloudinary for object storage

## Key Features

- **Automated Lead Capture:** Webhook integrations for Zalo OA and Facebook Fanpage.
- **Omnichannel Inbox:** Unified interface for messaging across different social platforms.
- **Kanban Pipeline:** Drag-and-drop management of student application stages.
- **Dynamic Checklists & Deadlines:** Automated tracking and cron-job alerts for document expirations.
- **Installment Tracking:** Financial module to break down consultation and school fees into trackable payment stages.
- **Role-Based Access Control:** Granular permissions for Counselors, Managers, and Administrators.

## Getting Started

### Prerequisites

- Java 17 or higher
- Node.js 18 or higher
- PostgreSQL 14+
- Docker & Docker Compose (optional for local deployment)

### Setup Instructions

1. **Clone the repository**
   ```bash
   git clone https://github.com/NguyenChien536/Korean-Pathway-CRM.git
   cd Korean-Pathway-CRM
   ```

2. **Database Setup**
   Configure your PostgreSQL instance and update the database credentials in the application properties.

3. **Backend Setup**
   ```bash
   cd backend
   ./mvnw clean install
   ./mvnw spring-boot:run
   ```

4. **Frontend Setup**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

## Documentation

Detailed requirements, use cases, and non-functional constraints can be found in the [SRSReport.md](SRSReport.md) file.
