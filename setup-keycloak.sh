#!/bin/bash

# Keycloak setup script to be run after Keycloak is up and running

KEYCLOAK_URL="http://localhost:8080"
ADMIN_USER="admin"
ADMIN_PASSWORD="admin"
REALM_NAME="business-monitoring"
CLIENT_ID="business-monitoring-client"

echo "Get admin access token"
ADMIN_TOKEN=$(curl -s -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=$ADMIN_USER" \
  -d "password=$ADMIN_PASSWORD" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | jq -r '.access_token')

if [ "$ADMIN_TOKEN" = "null" ] || [ -z "$ADMIN_TOKEN" ]; then
  echo "Failed to get admin token"
  exit 1
fi

echo "Creating realm: $REALM_NAME"
curl -s -X POST "$KEYCLOAK_URL/admin/realms" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"realm\": \"$REALM_NAME\",
    \"enabled\": true,
    \"displayName\": \"Business Monitoring Realm\",
    \"accessTokenLifespan\": 3600,
    \"refreshTokenMaxReuse\": 0,
    \"ssoSessionIdleTimeout\": 1800,
    \"ssoSessionMaxLifespan\": 36000
  }"


echo "Creating client: $CLIENT_ID"
curl -s -X POST "$KEYCLOAK_URL/admin/realms/$REALM_NAME/clients" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"clientId\": \"$CLIENT_ID\",
    \"enabled\": true,
    \"publicClient\": true,
    \"directAccessGrantsEnabled\": true,
    \"standardFlowEnabled\": true,
    \"implicitFlowEnabled\": false,
    \"protocol\": \"openid-connect\",
    \"redirectUris\": [
      \"http://localhost:8090/*\",
      \"http://app:8090/*\",
      \"https://insomnia.rest/*\",
      \"insomnia://oauth/callback\",
      \"https://oauth.pstmn.io/v1/callback\",
      \"http://localhost:*\"
    ],
    \"webOrigins\": [
      \"http://localhost:8090\",
      \"http://app:8090\",
      \"https://insomnia.rest\",
      \"http://localhost\"
    ],
    \"attributes\": {
      \"access.token.lifespan\": \"3600\"
    }
  }"

echo "Creating realm roles"
curl -s -X POST "$KEYCLOAK_URL/admin/realms/$REALM_NAME/roles" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"REPORT_USER\",
    \"description\": \"Report User Role\"
  }"


echo "Creating test report user"
curl -s -X POST "$KEYCLOAK_URL/admin/realms/$REALM_NAME/users" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"reportuser\",
    \"enabled\": true,
    \"email\": \"reportuser@example.com\",
    \"firstName\": \"Report\",
    \"lastName\": \"User\",
    \"credentials\": [{
      \"type\": \"password\",
      \"value\": \"password123\",
      \"temporary\": false
    }]
  }"


echo "Assigning role REPORT_USER to report user"

REPORT_USER_ID=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM_NAME/users?username=reportuser" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[0].id')

REPORT_ROLE_ID=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM_NAME/roles/REPORT_USER" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.id')

curl -s -X POST "$KEYCLOAK_URL/admin/realms/$REALM_NAME/users/$REPORT_USER_ID/role-mappings/realm" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "[{
    \"id\": \"$REPORT_ROLE_ID\",
    \"name\": \"REPORT_USER\"
  }]"

echo "Keycloak setup completed successfully!"