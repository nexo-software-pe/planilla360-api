# language: es
Característica: Gratificaciones de julio y diciembre
  Para pagar a cada trabajador lo que le corresponde por ley
  Como analista de RRHH
  Quiero que la gratificación sea proporcional a los meses completos trabajados en el semestre

  Antecedentes:
    Dado los parámetros legales de referencia

  Escenario: Semestre completo en el régimen general
    Dado un trabajador del régimen general con sueldo básico de 3000.00 soles y asignación familiar de 113.00 soles
    Y que trabajó 6 meses completos en el semestre
    Cuando se calcula su gratificación
    Entonces la gratificación es de 3113.00 soles
    Y la bonificación extraordinaria es de 280.17 soles
    Y recibe en total 3393.17 soles

  Esquema del escenario: La gratificación es proporcional a los meses completos
    Dado un trabajador del régimen general con sueldo básico de 3000.00 soles y asignación familiar de 113.00 soles
    Y que trabajó <meses> meses completos en el semestre
    Cuando se calcula su gratificación
    Entonces la gratificación es de <gratificacion> soles
    Y la bonificación extraordinaria es de <bonificacion> soles

    Ejemplos:
      | meses | gratificacion | bonificacion |
      | 0     | 0.00          | 0.00         |
      | 1     | 518.83        | 46.69        |
      | 3     | 1556.50       | 140.09       |
      | 4     | 2075.33       | 186.78       |
      | 5     | 2594.17       | 233.48       |

  Escenario: La pequeña empresa paga la mitad
    Dado un trabajador del régimen de pequeña empresa con sueldo básico de 3000.00 soles y asignación familiar de 113.00 soles
    Y que trabajó 6 meses completos en el semestre
    Cuando se calcula su gratificación
    Entonces la gratificación es de 1556.50 soles
    Y la bonificación extraordinaria es de 140.09 soles

  Escenario: Las horas extra de tres o más meses suben la remuneración computable
    Dado un trabajador del régimen general con sueldo básico de 3000.00 soles y asignación familiar de 0.00 soles
    Y que hizo horas extra por 300.00 soles en cada uno de 3 meses del semestre
    Y que trabajó 6 meses completos en el semestre
    Cuando se calcula su gratificación
    Entonces la gratificación es de 3150.00 soles

  Escenario: Las horas extra de menos de tres meses no se cuentan
    Dado un trabajador del régimen general con sueldo básico de 3000.00 soles y asignación familiar de 0.00 soles
    Y que hizo horas extra por 500.00 soles en cada uno de 2 meses del semestre
    Y que trabajó 6 meses completos en el semestre
    Cuando se calcula su gratificación
    Entonces la gratificación es de 3000.00 soles
