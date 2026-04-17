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

                    # Load environment variables from .env if it exists
                    if [ -f /root/.env ]; then
                        set -a
                        source /root/.env
                        set +a
                    fi

                    # Use environment variables or defaults
                    DB_PASSWORD=${DB_PASSWORD:-postgres}
                    JWT_SECRET=${JWT_SECRET:-your_secure_jwt_key_change_in_production}

                    docker run -d \
                        --name ${DOCKER_CONTAINER} \
                        --network llama_net \
                        -p ${APP_PORT}:8085 \
                        -e SPRING_PROFILES_ACTIVE=prod \
                        -e SPRING_DATASOURCE_URL=jdbc:postgresql://llama_db:5432/cash_organizer \
                        -e SPRING_DATASOURCE_USERNAME=postgres \
                        -e SPRING_DATASOURCE_PASSWORD="${DB_PASSWORD}" \
                        -e JWT_SECRET_KEY="${JWT_SECRET}" \
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
                    echo "Verifying backend health..."
                    MAX_ATTEMPTS=15
                    ATTEMPT=0

                    while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
                        if docker exec ${DOCKER_CONTAINER} curl -s http://localhost:8085/actuator/health 2>/dev/null | grep -q "UP"; then
                            echo "✓ Backend is healthy and responding"
                            docker exec ${DOCKER_CONTAINER} java -version 2>&1 | grep -i openjdk
                            exit 0
                        fi
                        ATTEMPT=$((ATTEMPT + 1))
                        echo "Attempt $ATTEMPT/$MAX_ATTEMPTS - waiting for application startup..."
                        sleep 3
                    done

                    echo "✗ Deployment verification failed after $MAX_ATTEMPTS attempts"
                    echo "Container logs:"
                    docker logs --tail=50 ${DOCKER_CONTAINER}
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
