# language: es
Característica: Compensación por tiempo de servicios (CTS)
  Para depositar la CTS de mayo y de noviembre
  Como analista de RRHH
  Quiero que se calcule sobre meses y días completos del semestre, con un sexto de la última gratificación

  Antecedentes:
    Dado los parámetros legales de referencia

  Escenario: Semestre completo en el régimen general
    Dado un trabajador del régimen general con sueldo básico de 3000.00 soles y asignación familiar de 113.00 soles
    Y que su última gratificación fue de 3113.00 soles
    Y que trabajó 6 meses y 0 días completos en el semestre
    Cuando se calcula su CTS
    Entonces su remuneración computable es de 3631.83 soles
    Y el depósito de CTS es de 1815.92 soles

  Esquema del escenario: La CTS suma los meses y los días completos
    Dado un trabajador del régimen general con sueldo básico de 3000.00 soles y asignación familiar de 113.00 soles
    Y que su última gratificación fue de 3113.00 soles
    Y que trabajó <meses> meses y <dias> días completos en el semestre
    Cuando se calcula su CTS
    Entonces el depósito de CTS es de <cts> soles

    Ejemplos:
      | meses | dias | cts     |
      | 0     | 0    | 0.00    |
      | 0     | 10   | 100.88  |
      | 1     | 0    | 302.65  |
      | 3     | 15   | 1059.28 |
      | 5     | 29   | 1805.83 |

  Escenario: La pequeña empresa deposita la mitad
    Dado un trabajador del régimen de pequeña empresa con sueldo básico de 3000.00 soles y asignación familiar de 113.00 soles
    Y que su última gratificación fue de 3113.00 soles
    Y que trabajó 6 meses y 0 días completos en el semestre
    Cuando se calcula su CTS
    Entonces el depósito de CTS es de 907.96 soles

  Escenario: Un trabajador nuevo sin gratificación previa solo cuenta básico y asignación
    Dado un trabajador del régimen general con sueldo básico de 3000.00 soles y asignación familiar de 113.00 soles
    Y que su última gratificación fue de 0.00 soles
    Y que trabajó 6 meses y 0 días completos en el semestre
    Cuando se calcula su CTS
    Entonces su remuneración computable es de 3113.00 soles
    Y el depósito de CTS es de 1556.50 soles
