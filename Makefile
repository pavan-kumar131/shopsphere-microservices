.PHONY: validate build test integration-test up down logs status seed clean
validate:
	python3 scripts/verify-static.py
	python3 -c 'import yaml; yaml.safe_load(open("docker-compose.yml"))'

build:
	docker compose build

test:
	@for service in auth-service product-service api-gateway order-service payment-service notification-service; do \
	  echo "== $$service tests =="; docker run --rm -v "$$PWD/$$service:/workspace" -w /workspace maven:3.9-eclipse-temurin-21 mvn -B test || exit $$?; \
	done

integration-test:
	@echo 'Run Docker Compose, then follow docs/LOCAL-VERIFICATION-CHECKLIST.md for checkout, RBAC, and ownership tests.'

up:
	cp -n .env.example .env || true
	docker compose up --build -d

down:
	docker compose down

logs:
	docker compose logs -f --tail=200

status:
	docker compose ps

seed:
	@echo 'Auth and product demo seed data is created by service startup. See README.md.'

clean:
	@echo 'No data is deleted. Use docker compose down -v only if you intentionally want to delete the local PostgreSQL volume.'
