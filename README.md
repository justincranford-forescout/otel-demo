# Greenfield OTLP Demo: Spring Boot => OpenTelemetry => Grafana

## Description
This is a demo of a Spring Boot REST application publishing Telemetry (x3) via OTLP to OpenTelemetry Collector, and forwarding to different vendor backends. Grafana Otel LGTM is used in this demo.

## High-level Data Flow:
1. Spring Boot Actuator
   1. Micrometer
      1. OpenTelemetry SDK (OTLP Protocol)
         1. OpenTelemetry Otel Collector
            1. Grafana Otel LGTM

## Terminology

### OpenTelemetry Protocol (OTLP)

OpenTelemetry Protocol (OTLP) is a vendor-neutral standard for collecting and transmitting telemetry data (traces, metrics, and logs) in distributed systems.

### Grafana Otel LGTM

The LGTM in Grafana Otel LGTM has a double meaning.

LGTM is most commonly used as an acronym in code reviews meaning `Looks Good To Me`.

In Grafana, LGTM is an acronym for four different telemetry components bundled in a single container image for `non-prod` (e.g. `dev`, `stg`, `demo`):
- `L`ogs (Loki) - Grafana logs database
- `G`UI (Grafana) - Grafana UI
- `M`etrics (Prometheus/Mimir) - Grafana metrics database
- `T`races (Tempo) - Grafana traces database

Grafana Pyroscope may also be used (for profiling), but the name is not included in the LGTM acronym.

## Spring Boot Versions & Limitations

Spring Boot depends on Micrometer for OpenTelemetry support.

Different versions of Spring Boot / Micrometer have different level of OTLP support.

- **Micrometer OTLP Support**
  - **Metrics** => `Spring Boot 3.0.0+`
    - HTTP only; GRPC still missing as of `Spring Boot 3.5.0`
  - **Traces** => `Spring Boot 3.4.0+`
    - HTTP and GRPC
  - **Logs** => `Spring Boot 3.4.0+`
    - HTTP and GRPC

## Telemetry Types & Data Flow Direction

- Logs: Push or Pull
- Metrics: Push or Pull
- Traces: Push

## Greenfield vs Brownfield

- Greenfield: All push
- Brownfield: Mixed; Metrics+Logs pull, Traces push

## Data Flow Detailed Options

- Metrics
  - Push
    - OTLP GRPC (TCP/4317)
    - OTLP HTTP (TCP/4317)
  - Pull
    - Prometheus Endpoint Scrape (TCP/9090)
- Traces
  - Push
    - OTLP GRPC (TCP/4317)
    - OTLP HTTP (TCP/4317)
- Logs
  - Push
      - OTLP GRPC (TCP/4317)
      - OTLP HTTP (TCP/4317)
  - Pull
      - Console STDOUT/STDERR Scrape

## External Links

1. https://github.com/open-telemetry/opentelemetry-collector/blob/main/README.md
    1. vendor-agnostic, and removes the need to run, operate and maintain multiple agents/collectors
    2. image: https://hub.docker.com/r/otel/opentelemetry-collector/tags
2. https://github.com/open-telemetry/opentelemetry-collector-contrib/blob/main/README.md
    1. extra components not available in the core collector, for example:
        1. Extra receivers: Kubernetes, PostgreSQL, Redis, SNMP, etc
        2. Extra exporters: Azure Monitor, GCP Trace, Elasticsearch, DataDog, Splunk, etc
    2. image: https://hub.docker.com/r/otel/opentelemetry-collector-contrib/tags
3. https://github.com/grafana/docker-otel-lgtm/blob/main/README.md
    1. intended for development, demo, and testing environments
    2. image: https://hub.docker.com/r/grafana/otel-lgtm/tags

## Background

OTLP is designed for efficient and flexible data ingest and delivery, supporting both gRPC and HTTP transports and binary serialization.

A single OTLP endpoint can ingest 3x telemetry types from many sources, and forward to Nx backends.

Consider Kubernetes VMs containing many mixed pods, with one OTLP endpoint per VM:
1. Pod = Spring Boot REST Application + Redis Cache
2. Pod = PostgreSQL Database
3. Pod = .Net Pubsub Application
4. DaemonSet = OpenTelemetry Collector

The containers in all pods can forward to a shared OpenTelemetry Collector endpoint on localhost:4317 (GRPC) or localhost:4317 (HTTP).

The OpenTelemetry Collector can then forward some or all to multiple Observability backends:
1. Grafana Otel LGTM (Dev Ops) => Retention 14d
2. Elasticsearch (SIEM) => Retention 30d
3. Honeycomb (Dev) => Retention 90d
4. FluentBit (Log Aggregation) => Retention 365d
