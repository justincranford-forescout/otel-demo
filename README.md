# Greenfield OTLP Demo: Spring Boot, OpenTelemetry, Grafana

## Description
The goal of this demo is to show a Spring Boot REST application publishing Telemetry (x3) via OTLP to OpenTelemetry Collector, and forwarding to at least one vendor-specific backend.
If you want more background, jump to the [Background](#background) section at the end.

## High-level Data Flow

1. Spring Boot Actuator
   1. Micrometer
      1. OpenTelemetry SDK (OTLP Protocol)
         1. OpenTelemetry Otel Collector
            1. Grafana Otel LGTM

## Terminology

### OpenTelemetry Protocol (OTLP)

OpenTelemetry Protocol (OTLP) is the Cloud Native Computing Foundation (CNCF) standard. See https://www.cncf.io/projects/opentelemetry/.

OTLP is a vendor-neutral protocol for distributed systems to collect and transmit telemetry data (i.e. traces, metrics, and logs). See https://opentelemetry.io/docs/.

### Grafana Otel LGTM

The LGTM in Grafana Otel LGTM has a double meaning. The common usage of LGTM is as an acronym in code reviews meaning `Looks Good To Me`.

In Grafana, LGTM is an acronym for four different telemetry components bundled in a single container image:
- `L`ogs (Loki) - Grafana logs database
- `G`UI (Grafana) - Grafana UI
- `M`etrics (Prometheus/Mimir) - Grafana metrics database
- `T`races (Tempo) - Grafana traces database

Grafana Otel LGTM is for `dev`, `stg`, and `demo` only. It is not intended for `prod` use.

## Spring Boot Versions & Limitations

Spring Boot used Micrometer for OpenTelemetry support. Different pairs of Spring Boot / Micrometer versions have different levels of OTLP support.

- **Micrometer OTLP Support**
  - **Metrics** => `Spring Boot 3.0.0+`
    - HTTP only; GRPC still not supported as of `Spring Boot 3.5.0`
  - **Traces** => `Spring Boot 3.4.0+`
    - HTTP and GRPC
  - **Logs** => `Spring Boot 3.4.0+`
    - HTTP and GRPC

Both protocols (HTTP and GRPC) encode data using Google Protocol Buffers (protobuf).
- GRPC is better for high-volume data transfer. It uses HTTP/2, and requires less CPU and memory.
- HTTP is better for low-volume data transfer. It is also easier to debug, and more compatible with traditional load balancers and proxies.

Example usage might be HTTP to OpenTelemetry Collector, then GRPC to Grafana Otel LGTM. That might help explain why Micrometer doesn't hack Metrics GRPC support (yet?).

## Telemetry Types & Data Flow Direction

### Options

- Logs: Push or Pull
- Metrics: Push or Pull
- Traces: Push; no known pull options exist

### High-level Data Flow: Greenfield vs Brownfield

- Greenfield: All push
- Brownfield: Traces push, Metrics+Logs fallback to traditional scrape/pull

### Detailed Data Flow: All Options

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
      - Container Console Scrape (STDOUT/STDERR)

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
