/* Copyright 2011 The Tor Project
 * See LICENSE for licensing information */
package org.torproject.onionoo;

import java.util.*;

/* Store search data containing those relays that have been running in the
 * past seven days. */
public class SearchData {
  public SortedSet<Long> getAllValidAfterMillis() {
    return new TreeSet<Long>(this.containedValidAfterMillis);
  }
  public long getLastValidAfterMillis() {
    return this.containedValidAfterMillis.isEmpty() ? -1L :
        this.containedValidAfterMillis.last();
  }
  public long getLastFreshUntilMillis() {
    return this.containedValidAfterMillis.isEmpty() ? -1L :
        this.containedValidAfterMillis.last() + 60L * 60L * 1000L;
  }
  private SortedSet<Long> containedValidAfterMillis = new TreeSet<Long>();
  private SortedMap<String, SearchEntryData> containedRelays =
      new TreeMap<String, SearchEntryData>();
  /* Add a search entry for a relay if this relay wasn't seen before or if
   * its current valid-after time is newer than the currently known
   * valid-after time. */
  private long now = System.currentTimeMillis();
  public void addRelay(String nickname, String fingerprint,
      String address, long validAfterMillis, int orPort, int dirPort,
      SortedSet<String> relayFlags) {
    if (validAfterMillis >= now - 7L * 24L * 60L * 60L * 1000L &&
        (!this.containedRelays.containsKey(fingerprint) ||
        this.containedRelays.get(fingerprint).getValidAfterMillis() <
        validAfterMillis)) {
      SearchEntryData entry = new SearchEntryData(nickname, fingerprint,
          address, validAfterMillis, orPort, dirPort, relayFlags);
      this.containedRelays.put(fingerprint, entry);
      this.containedValidAfterMillis.add(validAfterMillis);
    }
  }
  public SortedMap<String, SearchEntryData> getRelays() {
    return new TreeMap<String, SearchEntryData>(this.containedRelays);
  }
  public void updateAll(Collection<NetworkStatusData> consensuses) {
    if (consensuses != null) {
      for (NetworkStatusData consensus : consensuses) {
        this.update(consensus);
      }
    }
  }
  public void update(NetworkStatusData consensus) {
    long validAfterMillis = consensus.getValidAfterMillis();
    for (NetworkStatusEntryData entry :
        consensus.getStatusEntries().values()) {
      String nickname = entry.getNickname();
      String fingerprint = entry.getFingerprint();
      String address = entry.getAddress();
      int orPort = entry.getOrPort();
      int dirPort = entry.getDirPort();
      SortedSet<String> relayFlags = entry.getRelayFlags();
      this.addRelay(nickname, fingerprint, address, validAfterMillis,
          orPort, dirPort, relayFlags);
    }
  }
  private SortedSet<Long> containedPublishedMillis = new TreeSet<Long>();
  public void updateBridgeNetworkStatuses(
      Collection<BridgeNetworkStatusData> statuses) {
    if (statuses != null) {
      for (BridgeNetworkStatusData status : statuses) {
        long publishedMillis = status.getPublishedMillis();
        for (String hashedFingerprint : status.getStatusEntries()) {
          this.addBridge(hashedFingerprint, publishedMillis);
          this.containedPublishedMillis.add(publishedMillis);
        }
      }
    }
  }
  private SortedMap<String, Long> containedBridges =
      new TreeMap<String, Long>();
  public void addBridge(String hashedFingerprint, long publishedMillis) {
    if (publishedMillis >= now - 7L * 24L * 60L * 60L * 1000L &&
        (!this.containedBridges.containsKey(hashedFingerprint) ||
        this.containedBridges.get(hashedFingerprint) < publishedMillis)) {
      this.containedBridges.put(hashedFingerprint, publishedMillis);
    }
  }
  public SortedMap<String, Long> getBridges() {
    return new TreeMap<String, Long>(this.containedBridges);
  }
  public long getLastPublishedMillis() {
    return this.containedPublishedMillis.isEmpty() ? -1L :
        this.containedPublishedMillis.last();
  }
}

