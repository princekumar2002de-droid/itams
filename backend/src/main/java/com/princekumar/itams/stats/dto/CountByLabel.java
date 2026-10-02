package com.princekumar.itams.stats.dto;

/** A single "label + count" bucket for by-department, by-category, by-status breakdowns. */
public record CountByLabel(String label, long count) {}
