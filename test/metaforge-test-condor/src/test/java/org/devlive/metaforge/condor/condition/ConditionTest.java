package org.devlive.metaforge.condor.condition;

import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.condor.ComparisonOperator;
import org.devlive.metaforge.condor.metadata.RowDefinition;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Slf4j
public class ConditionTest
{
    private RowDefinition createRow(String name, int age, double score)
    {
        RowDefinition row = new RowDefinition();
        row.setValue("name", name);
        row.setValue("age", age);
        row.setValue("score", score);
        return row;
    }

    @Test
    public void testEquals()
    {
        SimpleCondition condition = new SimpleCondition("name", "Alice", ComparisonOperator.EQUALS);
        assertTrue(condition.evaluate(createRow("Alice", 25, 90.0)));
        assertFalse(condition.evaluate(createRow("Bob", 30, 85.0)));
    }

    @Test
    public void testNotEquals()
    {
        SimpleCondition condition = new SimpleCondition("name", "Alice", ComparisonOperator.NOT_EQUALS);
        assertFalse(condition.evaluate(createRow("Alice", 25, 90.0)));
        assertTrue(condition.evaluate(createRow("Bob", 30, 85.0)));
    }

    @Test
    public void testGreaterThan()
    {
        SimpleCondition condition = new SimpleCondition("age", 25, ComparisonOperator.GREATER_THAN);
        assertFalse(condition.evaluate(createRow("Alice", 25, 90.0)));
        assertTrue(condition.evaluate(createRow("Bob", 30, 85.0)));
        assertFalse(condition.evaluate(createRow("Charlie", 20, 95.0)));
    }

    @Test
    public void testGreaterThanOrEquals()
    {
        SimpleCondition condition = new SimpleCondition("age", 25, ComparisonOperator.GREATER_THAN_OR_EQUALS);
        assertTrue(condition.evaluate(createRow("Alice", 25, 90.0)));
        assertTrue(condition.evaluate(createRow("Bob", 30, 85.0)));
        assertFalse(condition.evaluate(createRow("Charlie", 20, 95.0)));
    }

    @Test
    public void testLessThan()
    {
        SimpleCondition condition = new SimpleCondition("age", 30, ComparisonOperator.LESS_THAN);
        assertTrue(condition.evaluate(createRow("Alice", 25, 90.0)));
        assertFalse(condition.evaluate(createRow("Bob", 30, 85.0)));
        assertFalse(condition.evaluate(createRow("Charlie", 35, 95.0)));
    }

    @Test
    public void testLessThanOrEquals()
    {
        SimpleCondition condition = new SimpleCondition("age", 30, ComparisonOperator.LESS_THAN_OR_EQUALS);
        assertTrue(condition.evaluate(createRow("Alice", 25, 90.0)));
        assertTrue(condition.evaluate(createRow("Bob", 30, 85.0)));
        assertFalse(condition.evaluate(createRow("Charlie", 35, 95.0)));
    }

    @Test
    public void testDoubleComparison()
    {
        SimpleCondition condition = new SimpleCondition("score", 90.0, ComparisonOperator.GREATER_THAN);
        assertFalse(condition.evaluate(createRow("Alice", 25, 90.0)));
        assertTrue(condition.evaluate(createRow("Charlie", 20, 95.0)));
        assertFalse(condition.evaluate(createRow("Bob", 30, 85.0)));
    }

    @Test
    public void testNullValue()
    {
        RowDefinition row = new RowDefinition();
        row.setValue("name", null);
        row.setValue("age", 25);

        SimpleCondition condition = new SimpleCondition("name", "Alice", ComparisonOperator.EQUALS);
        assertFalse(condition.evaluate(row));
    }

    @Test
    public void testBothNull()
    {
        RowDefinition row = new RowDefinition();
        row.setValue("name", null);

        SimpleCondition condition = new SimpleCondition("name", null, ComparisonOperator.EQUALS);
        assertTrue(condition.evaluate(row));
    }

    @Test
    public void testAndCondition()
    {
        SimpleCondition ageCondition = new SimpleCondition("age", 20, ComparisonOperator.GREATER_THAN);
        SimpleCondition scoreCondition = new SimpleCondition("score", 90.0, ComparisonOperator.GREATER_THAN_OR_EQUALS);
        AndCondition andCondition = new AndCondition(ageCondition, scoreCondition);

        assertTrue(andCondition.evaluate(createRow("Alice", 25, 90.0)));
        assertFalse(andCondition.evaluate(createRow("Bob", 30, 85.0)));
        assertFalse(andCondition.evaluate(createRow("Charlie", 18, 95.0)));
    }

    @Test
    public void testOrCondition()
    {
        SimpleCondition nameCondition = new SimpleCondition("name", "Alice", ComparisonOperator.EQUALS);
        SimpleCondition ageCondition = new SimpleCondition("age", 35, ComparisonOperator.EQUALS);
        OrCondition orCondition = new OrCondition(nameCondition, ageCondition);

        assertTrue(orCondition.evaluate(createRow("Alice", 25, 90.0)));
        assertTrue(orCondition.evaluate(createRow("Charlie", 35, 95.0)));
        assertFalse(orCondition.evaluate(createRow("Bob", 30, 85.0)));
    }

    @Test
    public void testNestedConditions()
    {
        // (name = 'Alice' AND age > 20) OR score >= 95
        SimpleCondition nameCondition = new SimpleCondition("name", "Alice", ComparisonOperator.EQUALS);
        SimpleCondition ageCondition = new SimpleCondition("age", 20, ComparisonOperator.GREATER_THAN);
        AndCondition andCondition = new AndCondition(nameCondition, ageCondition);

        SimpleCondition scoreCondition = new SimpleCondition("score", 95.0, ComparisonOperator.GREATER_THAN_OR_EQUALS);
        OrCondition orCondition = new OrCondition(andCondition, scoreCondition);

        assertTrue(orCondition.evaluate(createRow("Alice", 25, 80.0)));
        assertTrue(orCondition.evaluate(createRow("Bob", 30, 95.0)));
        assertFalse(orCondition.evaluate(createRow("Bob", 30, 85.0)));
        assertFalse(orCondition.evaluate(createRow("Alice", 18, 80.0)));
    }

    @Test
    public void testCrossTypeNumericComparison()
    {
        RowDefinition row = new RowDefinition();
        row.setValue("value", 10);

        // Integer vs Long comparison
        SimpleCondition condition = new SimpleCondition("value", 5L, ComparisonOperator.GREATER_THAN);
        assertTrue(condition.evaluate(row));
    }

    @Test
    public void testStringComparison()
    {
        SimpleCondition condition = new SimpleCondition("name", "Bob", ComparisonOperator.LESS_THAN);
        assertTrue(condition.evaluate(createRow("Alice", 25, 90.0)));
        assertFalse(condition.evaluate(createRow("Charlie", 30, 85.0)));
    }
}
