using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using static UnitController;

public class PlayerBehaviour : MonoBehaviour
{
    public float moveSpeed = 5f;
    public float jumpForce = 10f;  // ���� ������
    public float summonRadius = 1.5f; // ������ ������ ������
    public UnitsZone[] zones; // ���� ��� ����� �����
    private List<UnitController> controlledUnits = new List<UnitController>();
    private bool _unitsInIdle = false;

    private Rigidbody2D rb;  // ������ �� Rigidbody2D
    private bool isGrounded;  // ���� ��� ��������, ��������� �� �������� �� �����
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
        rb.velocity = new Vector2(move * moveSpeed, rb.velocity.y);  // ������������� �������� �� ��� X � Y

        // ������������ ��������� � ������� ��������
        if (move != 0)
        {
            Vector3 scale = transform.localScale;
            scale.x = move > 0 ? 1 : -1;  // ������������ ��������� � ����������� �� �����������
            transform.localScale = scale;
        }
    }

    void Jump()
    {
        // �������� ������� ������� ��� ������ � ��������, ��������� �� �������� �� �����
        if (Input.GetKeyDown(KeyCode.Space) && isGrounded)
        {
            rb.AddForce(new Vector2(0, jumpForce), ForceMode2D.Impulse);
            isGrounded = false; // �������� ������ � �������
        }
    }

    private void OnCollisionEnter2D(Collision2D collision)
    {
        if (collision.gameObject.CompareTag("Ground")) // ��������� ������������ � ��������, ���������� ��� "Ground"
        {
            isGrounded = true; // �������� ����� �� �����
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
                    UnitsZone targetZone = GetZoneForUnit(unit); // �������� ���� �� ���� �����
                    if (targetZone != null)
                    {
                        Transform freePosition = targetZone.SetUnit(); // �������� ��������� ����� � ����
                        if (freePosition != null) // ���� ���� �� ���������
                        {
                            unit.ControlledByPlayer(this);
                            controlledUnits.Add(unit);
                            unit.SetPosition(freePosition); // ������������� ������� �����
                        }
                    }
                }
            }
        }
    }

    UnitsZone GetZoneForUnit(UnitController unit)
    {
        // ����� �� ������ ����������, ����� ���� �������� ��� ������� ���� �����
        switch (unit.unitType)
        {
            case UnitType.Spearman:
                return zones[0]; // ���� ��� ������ ���� A
            case UnitType.Archer:
                return zones[1]; // ���� ��� ������ ���� B
            // �������� ������ ���� ������ � ���� �� ���� �������������
            default:
                return zones[2]; // ����������� ���, �� �������� �����
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
