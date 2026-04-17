pipeline {
    agent any

    // Automatic triggers
    triggers {
        githubPush()
        pollSCM('H/5 * * * *')
    }

    environment {
        DOCKER_IMAGE = 'cash-organizer-api'
        DOCKER_CONTAINER = 'cash-organizer-api'
        APP_PORT = '8085'
        REPO_URL = 'https://github.com/Eunonti4815162342/cash_organizer_api.git'
        GIT_BRANCH = 'master'
    }

    stages {
        stage('Checkout') {
            steps {
                cleanWs()
                git branch: "${GIT_BRANCH}",
                    credentialsId: 'cash_organizer',
                    url: "${REPO_URL}"
                sh "echo '✓ Repository checked out'"
            }
        }

        stage('Verify Build Files') {
            steps {
                sh '''
                    echo "Verifying build files..."
                    test -f Dockerfile || (echo "ERROR: Dockerfile not found!" && exit 1)
                    test -f build.gradle || (echo "ERROR: build.gradle not found!" && exit 1)
                    test -f gradlew || (echo "ERROR: gradlew not found!" && exit 1)
                    echo "✓ All build files verified"
                '''
            }
        }

        stage('Build Docker Image') {
            steps {
                sh '''
                    echo "Building Docker image: ${DOCKER_IMAGE}:latest"
                    docker build \
                        --network=host \
                        -t ${DOCKER_IMAGE}:latest \
                        -t ${DOCKER_IMAGE}:build-${BUILD_NUMBER} \
                        .
                    echo "✓ Docker image built successfully"
                    docker images ${DOCKER_IMAGE}
                '''
            }
        }

        stage('Stop & Remove Old Container') {
            steps {
                sh '''
                    echo "Stopping and removing old container..."
                    docker stop ${DOCKER_CONTAINER} 2>/dev/null || true
                    docker rm ${DOCKER_CONTAINER} 2>/dev/null || true
                    echo "✓ Old container removed"
                '''
            }
        }

        stage('Deploy Container') {
            steps {
                sh '''
                    echo "Deploying new backend container..."

                    # Use environment variables from Jenkins (set in docker-compose)
                    DB_HOST=${DB_POSTGRESDB_HOST:-llama_db}
                    DB_PORT=${DB_POSTGRESDB_PORT:-5432}
                    DB_NAME=${DB_POSTGRESDB_DATABASE:-llama_finanzas}
                    DB_USER=${DB_POSTGRESDB_USER:-llama_user}
                    DB_PASSWORD=${DB_POSTGRESDB_PASSWORD:-llama_password}

                    echo "Deploying backend to: ${DB_HOST}:${DB_PORT}/${DB_NAME}"

                    docker run -d \
                        --name ${DOCKER_CONTAINER} \
                        --network llama_net \
                        -p ${APP_PORT}:8085 \
                        -e SPRING_PROFILES_ACTIVE=prod \
                        -e SPRING_DATASOURCE_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}" \
                        -e SPRING_DATASOURCE_USERNAME="${DB_USER}" \
                        -e SPRING_DATASOURCE_PASSWORD="${DB_PASSWORD}" \
                        -e JWT_SECRET_KEY=change_in_production \
                        --health-cmd="curl -f http://localhost:8085/actuator/health || exit 1" \
                        --health-interval=30s \
                        --health-timeout=10s \
                        --health-retries=3 \
                        --restart unless-stopped \
                        ${DOCKER_IMAGE}:latest

                    echo "✓ Container deployed with ID: $(docker ps -q -f name=${DOCKER_CONTAINER})"
                    sleep 5
                '''
            }
        }

        stage('Verify Deployment') {
            steps {
                sh '''
                    echo "Waiting for backend to fully start (Liquibase migrations + Spring Boot)..."
                    echo "Showing container logs in real-time:"
                    echo "================================================================"

                    # Show logs with timeout of 5 minutes
                    timeout 300 docker logs -f ${DOCKER_CONTAINER} &
                    LOGS_PID=$!

                    # Wait for Spring Boot + Liquibase to fully start (critical on Raspberry Pi)
                    sleep 120
                    for i in {1..20}; do
                        # Check health from inside container using wget (which is available in alpine)
                        HEALTH=$(docker exec ${DOCKER_CONTAINER} wget -q -O- http://localhost:8085/actuator/health 2>/dev/null)
                        if echo "$HEALTH" | grep -q "UP"; then
                            kill $LOGS_PID 2>/dev/null || true
                            echo "================================================================"
                            echo "✅ Backend is healthy!"
                            echo "Health: $HEALTH"
                            echo ""
                            docker ps -f name=${DOCKER_CONTAINER} --format "Container: {{.Names}} | Status: {{.Status}} | Ports: {{.Ports}}"
                            exit 0
                        fi
                        echo "Waiting... ($((i*10)) seconds elapsed)"
                        sleep 10
                    done

                    kill $LOGS_PID 2>/dev/null || true
                    echo "================================================================"
                    echo "✗ Backend health check failed after 5 minutes"
                    echo "Checking container logs for errors..."
                    docker logs --tail=20 ${DOCKER_CONTAINER}
                    exit 1
                '''
            }
        }
    }

    post {
        success {
            sh '''
                echo "╔════════════════════════════════════════╗"
                echo "║  ✓ BACKEND DEPLOYMENT SUCCESSFUL       ║"
                echo "╚════════════════════════════════════════╝"
                echo ""
                echo "Backend API: http://192.168.1.145:${APP_PORT}"
                echo "Health:      http://192.168.1.145:${APP_PORT}/actuator/health"
                echo ""
                echo "Container Status:"
                docker ps -f name=${DOCKER_CONTAINER} --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
            '''
        }
        failure {
            sh '''
                echo "╔════════════════════════════════════════╗"
                echo "║  ✗ BACKEND DEPLOYMENT FAILED           ║"
                echo "╚════════════════════════════════════════╝"
                echo ""
                echo "Container logs:"
                docker logs --tail=100 ${DOCKER_CONTAINER} || echo "Container not running"
                echo ""
                echo "Running containers:"
                docker ps -a --format "table {{.Names}}\t{{.Status}}"
            '''
        }
        always {
            cleanWs()
        }
    }
}
