# Personal Finance Tracker - Backend

This is the backend REST API for the Personal Finance Tracker, built with Spring Boot. It handles authentication, user management, transactions, categories, menu management, and more.

## Features

- RESTful API for all finance tracker features
- JWT authentication with refresh tokens
- 2FA support for enhanced security
- Role-based access control (User, Admin, Accountant)
- Transaction and category management
- CSV import/export endpoints
- Menu management for dynamic frontend navigation
- Connection pool and cache monitoring endpoints
- Optimistic locking for user/profile updates

## Getting Started

### Prerequisites

- Java 17+
- Maven

### Installation

1. Navigate to the backend directory:

   ```bash
   cd personal-finance-tracker-backend