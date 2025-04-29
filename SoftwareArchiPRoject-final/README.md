# Container Monitoring System

A comprehensive monitoring solution for a Message Queue (MQ) based application using Spring Boot, Prometheus, Grafana, and Discord alerts. The system processes messages between services and monitors the entire message flow.

## System Architecture

The system consists of several components:
- 2 Spring Boot applications (app1 and app2) with message processing capabilities
- Message Queue system for inter-service communication
- Nginx load balancer
- PostgreSQL database
- Monitoring service
- Prometheus for metrics collection
- Grafana for visualization and alerting
- Discord integration for notifications

### Components Overview

1. **Spring Boot Applications with Message Queue**
   - Two identical instances (app1 and app2)
   - Ports: 8081 and 8082
   - Connected to PostgreSQL database
   - Message processing capabilities:
     - Sends and receives messages through queues
     - Processes messages asynchronously
     - Handles message failures and retries
     - Tracks message processing metrics

2. **Message Queue Monitoring**
   - Tracks message processing rates
   - Monitors queue sizes
   - Measures processing latency
   - Tracks failed messages
   - Provides message flow visualization

3. **Nginx Load Balancer**
   - Distributes traffic between app1 and app2
   - Port: 80

4. **Monitoring Service**
   - Tracks container health
   - Manages container lifecycle
   - Monitors message queue health
   - Port: 8083

5. **Prometheus**
   - Collects metrics from all services
   - Tracks message-related metrics:
     - Messages processed per second
     - Queue depth
     - Processing time
     - Error rates
   - Port: 9090

6. **Grafana**
   - Visualizes metrics
   - Manages alerts
   - Provides message queue dashboards
   - Port: 3000

## Message Queue Metrics

The system tracks several message-related metrics:

1. **Processing Metrics**
   - Messages processed per second
   - Average processing time
   - Success/failure rates
   - Queue depth over time

2. **Error Tracking**
   - Failed message count
   - Error types distribution
   - Retry attempts
   - Dead letter queue size

3. **Performance Metrics**
   - Queue latency
   - Processing backlog
   - Consumer lag
   - Resource utilization during processing

## Dashboard Panels

The Grafana dashboard includes specific panels for message monitoring:

1. **Messages Processed (top left)**
   - Real-time message processing rate
   - Historical processing trends
   - Success/failure ratio

2. **Failed Messages (top right)**
   - Failed message count
   - Error type distribution
   - Retry statistics

3. **CPU Usage (bottom left)**
   - CPU utilization per service
   - Processing impact on CPU
   - Resource usage patterns

4. **Memory Usage (bottom right)**
   - Heap memory usage
   - Memory patterns during processing
   - GC impact on processing

## Alert Rules

The system includes preconfigured alert rules:

1. **Container Down Alert**
   - Triggers when any application container stops
   - 30-second evaluation period
   - Discord notification with container details

2. **Message Processing Alerts**
   - Queue depth exceeds threshold
   - Processing time anomalies
   - High failure rate detection
   - Dead letter queue alerts

## Running the System

### Prerequisites
- Docker and Docker Compose installed
- Discord webhook URL (for alerts)
- Git (for cloning the repository)
- Ports 80, 3000, 8081-8083, 9090 available

### Initial Setup

1. **Clone and Configure**
   ```bash
   # Clone the repository
   git clone <repository-url>
   cd <repository-name>

   # Create .env file
   echo "DISCORD_WEBHOOK_URL=your_webhook_url" > .env
   ```

2. **Start the System**
   ```bash
   # Build and start all containers
   docker-compose up -d

   # Verify all containers are running
   docker-compose ps
   ```

3. **Access Points**
   - Main Application: http://localhost:80
   - Grafana Dashboard: http://localhost:3000 (admin/admin)
   - Prometheus: http://localhost:9090

3. **Test Alert System**
   ```bash
   # Stop one container to trigger alert
   docker-compose stop app1

   # Restart the container
   docker-compose start app1
   ```

### Monitoring the System

1. **Grafana Dashboards**
   - Open http://localhost:3000
   - Login with admin/admin
   - Navigate to "Messaging System Dashboard"
   - Check all four panels:
     - Messages Processed
     - Failed Messages
     - CPU Usage
     - Memory Usage

