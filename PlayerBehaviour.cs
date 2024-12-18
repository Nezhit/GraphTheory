using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using static UnitController;

public class PlayerBehaviour : MonoBehaviour
{
    public float moveSpeed = 5f;
    public float jumpForce = 10f;  // Сила прыжка
    public float summonRadius = 1.5f; // Радиус поиска юнитов
    public UnitsZone[] zones; // Зоны для смены строя
    private List<UnitController> controlledUnits = new List<UnitController>();
    private bool _unitsInIdle = false;

    private Rigidbody2D rb;  // Ссылка на Rigidbody2D
    private bool isGrounded;  // Флаг для проверки, находится ли персонаж на земле
    private float move;
    private void Start()
    {
        rb = GetComponent<Rigidbody2D>();
    }

    private void Update()
    {
        Jump();
        move = Input.GetAxis("Horizontal");

        if (Input.GetKeyDown(KeyCode.E)) SummonUnits();
        if (Input.GetKeyDown(KeyCode.W)) ChangeFormation();
        if (Input.GetKeyDown(KeyCode.Q)) GiveOrder();
    }
    private void FixedUpdate()
    {
        Move();

    }
    void Move()
    {
        rb.velocity = new Vector2(move * moveSpeed, rb.velocity.y);  // Устанавливаем скорости на оси X и Y

        // Поворачиваем персонажа в сторону движения
        if (move != 0)
        {
            Vector3 scale = transform.localScale;
            scale.x = move > 0 ? 1 : -1;  // Поворачиваем персонажа в зависимости от направления
            transform.localScale = scale;
        }
    }

    void Jump()
    {
        // Проверка нажатия пробела для прыжка и проверка, находится ли персонаж на земле
        if (Input.GetKeyDown(KeyCode.Space) && isGrounded)
        {
            rb.AddForce(new Vector2(0, jumpForce), ForceMode2D.Impulse);
            isGrounded = false; // Персонаж теперь в воздухе
        }
    }

    private void OnCollisionEnter2D(Collision2D collision)
    {
        if (collision.gameObject.CompareTag("Ground")) // Проверяем столкновение с объектом, помеченным как "Ground"
        {
            isGrounded = true; // Персонаж снова на земле
        }
    }

    void SummonUnits()
    {
        Collider2D[] colliders = Physics2D.OverlapCircleAll(transform.position, summonRadius);
        foreach (var col in colliders)
        {
            if (col.CompareTag("Unit"))
            {
                UnitController unit = col.GetComponent<UnitController>();
                if (unit != null && !controlledUnits.Contains(unit))
                {
                    UnitsZone targetZone = GetZoneForUnit(unit); // Получаем зону по типу юнита
                    if (targetZone != null)
                    {
                        Transform freePosition = targetZone.SetUnit(); // Пытаемся поместить юнита в зону
                        if (freePosition != null) // Если зона не заполнена
                        {
                            unit.ControlledByPlayer(this);
                            controlledUnits.Add(unit);
                            unit.SetPosition(freePosition); // Устанавливаем позицию юнита
                        }
                    }
                }
            }
        }
    }

    UnitsZone GetZoneForUnit(UnitController unit)
    {
        // Здесь вы можете определить, какая зона подходит для данного типа юнита
        switch (unit.unitType)
        {
            case UnitType.Spearman:
                return zones[0]; // Зона для юнитов типа A
            case UnitType.Archer:
                return zones[1]; // Зона для юнитов типа B
            // Добавьте другие типы юнитов и зоны по мере необходимости
            default:
                return zones[2]; // Неизвестный тип, не помещаем юнита
        }
    }

    void ChangeFormation()
    {
        if (controlledUnits.Count == 0) return;
        for (int i = zones.Length - 1; i > 0; i--)
        {
            Debug.Log($"From {i} to {i - 1}");
            Vector3 tempPos = zones[i].transform.position;

            zones[i].transform.position = zones[i - 1].transform.position;
            zones[i - 1].transform.position = tempPos;
        }
    }

    void GiveOrder()
    {
        if (!_unitsInIdle)
        {
            if (controlledUnits.Count > 0)
            {
                foreach (var unit in controlledUnits)
                {
                    unit.GoIdle();
                }
                _unitsInIdle = true;
            }
        }
        else
        {
            if (controlledUnits.Count > 0)
            {
                foreach (var unit in controlledUnits)
                {
                    unit.GoForPlayer();
                }
                _unitsInIdle = false;
            }
        }
    }
}
