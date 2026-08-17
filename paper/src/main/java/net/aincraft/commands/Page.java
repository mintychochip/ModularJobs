package net.aincraft.commands;

import java.util.List;

/**
 * One page of a paginated list (e.g. job leaderboard entries).
 *
 * @param data       entries on this page
 * @param pageNumber 1-based page number (clamped to valid range)
 * @param size       maximum entries per page
 */
public record Page<T>(List<T> data, int pageNumber, int size) {

}
