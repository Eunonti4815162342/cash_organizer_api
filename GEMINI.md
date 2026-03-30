# Mandatos del Experto en Arquitectura de Software (Backend Java)

Este backend actúa como el motor central de "Cash Organizer", encargado de procesar la lógica de negocio extraída del código original descompilado. Como arquitecto jefe, establezco las siguientes normas:

## 🏛️ Arquitectura y Principios de Ingeniería
- **Arquitectura Hexagonal (Puertos y Adaptadores):** El dominio debe estar en el centro, rodeado por puertos (interfaces) y adaptadores (infraestructura como JPA y REST controllers). La lógica de negocio no debe depender de la base de datos ni de las APIs externas.
- **Domain-Driven Design (DDD):** Modelaremos el negocio según los agregados y entidades identificadas en la APK. El dominio debe ser rico en lógica, no solo contenedores de datos (evitar el Anemic Domain Model).
- **Principios SOLID:** Implementación rigurosa de SOLID para garantizar que el sistema sea fácil de mantener, testear y extender. La inyección de dependencias será el mecanismo principal para desacoplar componentes (DIP).

## 🧩 Traducción de Modelos (Semántica)
- **Desofuscación Obligatoria:** Las variables originales (`a`, `b`, `c`, `d`, etc.) DEBEN ser traducidas a nombres semánticos claros basándose en el análisis de los métodos de acceso (`getters/setters`) del código JADX.
- **Integridad de Datos:** Los modelos JPA deben respetar la estructura relacional observada en el código original, incluyendo objetos embebidos como `Amount`.

## 🛠️ Estándares de Entrega
- **Compatibilidad API:** Los endpoints deben devolver JSON estructurado que el frontend de Flutter pueda consumir eficientemente.
- **CORS:** El backend debe permitir peticiones cruzadas para facilitar el desarrollo multiplataforma.

## 📊 Estado Actual del Sistema
1. **Infraestructura Base:** Proyecto Spring Boot configurado con JPA, H2 y Lombok.
2. **Entidades Core:** Migración exitosa de `AccountItem` y `TransactionItem` desde el código Java ofuscado a entidades semánticas.

## 🚀 Próximos Pasos (Prioridad Alta)
1. **Refactorización Hexagonal:** Reestructurar los paquetes actuales (`model`, `repository`, `controller`) hacia una estructura DDD (`domain`, `application`, `infrastructure`).
2. **Lógica de Categorías:** Migración del modelo `CategoryItem.java` para habilitar la clasificación de gastos/ingresos.
3. **Servicios de Negocio:** Implementar validaciones y cálculos de saldo que originalmente residían en la capa `core` de la APK.
