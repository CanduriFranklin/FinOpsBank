# Multimodule Architecture (Clean Architecture)

To scale FinOpsBank to an enterprise level, the monolith is divided into three physical layers:

1. **Domain (domain/)**: Contains financial entities and use cases. Zero external framework dependencies.
2. **Infrastructure (infrastructure/)**: Contains technical implementations (databases, message brokers). Depends on domain.
3. **App (pp/)**: The entry point (CLI or REST). Depends on domain and infrastructure to orchestrate the application.
