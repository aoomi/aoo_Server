#!/usr/bin/env ruby
# frozen_string_literal: true

require 'minitest/autorun'
require_relative '../../audit/tool-inputs/v12_performance_stability_gate'

class AooV12PerformanceStabilityGateTest < Minitest::Test
  def bounded?(source)
    AooV12PerformanceStabilityGate.bounded_gateway_queue?(source)
  end

  def test_accepts_array_blocking_queue_with_positive_capacity
    assert bounded?('var queue = new ArrayBlockingQueue<>(512);')
    assert bounded?('var queue = new ArrayBlockingQueue<Job>(1);')
  end

  def test_rejects_array_blocking_queue_without_positive_capacity
    refute bounded?('var queue = new ArrayBlockingQueue<>();')
    refute bounded?('var queue = new ArrayBlockingQueue<>(0);')
    refute bounded?('var queue = new ArrayBlockingQueue<>(-1);')
  end

  def test_accepts_netty_group_only_with_positive_capacity_and_reject_policy
    source = <<~JAVA
      new DefaultEventExecutorGroup(8,
          Thread.ofPlatform().name("gateway-authority-", 0).factory(),
          4096,
          RejectedExecutionHandlers.reject())
    JAVA
    assert bounded?(source)
  end

  def test_rejects_netty_group_without_capacity_or_reject_policy
    refute bounded?('new DefaultEventExecutorGroup(8, new DefaultThreadFactory("authority"))')
    refute bounded?(
      'new DefaultEventExecutorGroup(8, Thread.ofPlatform().name("authority-", 0).factory(), 0, RejectedExecutionHandlers.reject())'
    )
    refute bounded?(
      'new DefaultEventExecutorGroup(8, Thread.ofPlatform().name("authority-", 0).factory(), -1, RejectedExecutionHandlers.reject())'
    )
    refute bounded?(
      'new DefaultEventExecutorGroup(8, Thread.ofPlatform().name("authority-", 0).factory(), 4096, fallbackHandler)'
    )
  end

  def test_ignores_qualifying_text_inside_comments
    refute bounded?('// new ArrayBlockingQueue<>(512)')
    refute bounded?('/* new DefaultEventExecutorGroup(8, factory, 4096, RejectedExecutionHandlers.reject()) */')
  end
end
