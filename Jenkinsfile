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
                    echo "Verifying backend health..."
                    MAX_ATTEMPTS=40
                    ATTEMPT=0

                    while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
                        # Check if port 8085 is listening (works without curl in alpine)
                        if docker exec ${DOCKER_CONTAINER} sh -c 'exec 3<>/dev/tcp/localhost/8085 && exec 3>&- && exec 3<&-' 2>/dev/null; then
                            echo "✓ Backend port 8085 is responding"
                            echo "✓ Container status:"
                            docker ps -f name=${DOCKER_CONTAINER} --format "Status: {{.Status}}"
                            exit 0
                        fi
                        ATTEMPT=$((ATTEMPT + 1))
                        echo "Attempt $ATTEMPT/$MAX_ATTEMPTS - waiting for application startup..."
                        sleep 2
                    done

                    echo "✗ Deployment verification failed after $MAX_ATTEMPTS attempts ($(($MAX_ATTEMPTS * 2)) seconds)"
                    echo "Container logs (last 100 lines):"
                    docker logs --tail=100 ${DOCKER_CONTAINER}
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
