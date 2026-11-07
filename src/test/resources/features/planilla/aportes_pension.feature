# language: es
Característica: Aportes al sistema de pensiones
  Para que la boleta muestre el descuento correcto
  Como analista de RRHH
  Quiero que el aporte se calcule según el sistema pensionario elegido por el trabajador

  Antecedentes:
    Dado los parámetros legales de referencia

  Escenario: Un trabajador de la ONP aporta el 13 % de su remuneración asegurable
    Dado un trabajador con remuneración asegurable de 3000.00 soles
    Y que está afiliado a la ONP
    Cuando se calcula su descuento pensionario
    Entonces el descuento total es de 390.00 soles

  Esquema del escenario: Un trabajador de una AFP aporta al fondo, paga la prima de seguro y la comisión
    Dado un trabajador con remuneración asegurable de <base> soles
    Y que está afiliado a la AFP <afp> con comisión <tipo> de <porcentaje> %
    Cuando se calcula su descuento pensionario
    Entonces el aporte al fondo es de <aporte> soles
    Y la prima de seguro es de <prima> soles
    Y la comisión es de <comision> soles
    Y el descuento total es de <total> soles

    Ejemplos:
      | base    | afp       | tipo   | porcentaje | aporte | prima | comision | total  |
      | 3000.00 | Prima     | flujo  | 1.60       | 300.00 | 41.10 | 48.00    | 389.10 |
      | 3000.00 | Prima     | mixta  | 1.25       | 300.00 | 41.10 | 37.50    | 378.60 |
      | 1130.00 | Integra   | flujo  | 1.55       | 113.00 | 15.48 | 17.52    | 146.00 |
      | 2500.00 | Profuturo | flujo  | 1.69       | 250.00 | 34.25 | 42.25    | 326.50 |
      | 4000.00 | Habitat   | mixta  | 1.25       | 400.00 | 54.80 | 50.00    | 504.80 |

  Escenario: La gratificación no paga aportes de pensión
    Dado un trabajador con remuneración asegurable de 3113.00 soles
    Y que está afiliado a la ONP
    Cuando se calcula su descuento pensionario
    Entonces el descuento total es de 404.69 soles
