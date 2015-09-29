/*
 * Copyright 2015 Google Inc. All rights reserved.
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.google.maps;

/**
 * Performs a text search for places. The Google Places API enables you to get data from the same
 * database used by Google Maps and Google+ Local. Places features more than 100 million businesses
 * and points of interest that are updated frequently through owner-verified listings and
 * user-moderated contributions.
 *
 * <p>See also: <a href="https://developers.google.com/places/web-service/">Places API Web Service
 * documentation</a>.
 */
public class PlacesApi {

  private PlacesApi() {
  }

  /**
   * Perform a search for Places using a text query — for example "pizza in New York" or
   * "shoe stores near Ottawa".
   *
   * @param context The context on which to make Geo API requests.
   * @param query The text string on which to search, for example: "restaurant".
   * @return Returns a TextSearchRequest that can  be configured and executed.
   */
  public static TextSearchRequest textSearchQuery(GeoApiContext context, String query) {
    TextSearchRequest request = new TextSearchRequest(context);
    request.query(query);
    return request;
  }

  /**
   * Retrieve the next page of Text Search results. The nextPageToken, returned in a PlaceSearchResponse
   * when there are more pages of results, encodes all of the original Text Search Request parameters, and are thus
   * not required on this call.
   *
   * @param context The context on which to make Geo API requests.
   * @param nextPageToken The nextPageToken returned as part of a PlaceSearchResponse.
   * @return Returns a TextSearchRequest that can be executed.
   */
  public static TextSearchRequest textSearchNextPage(GeoApiContext context, String nextPageToken) {
    TextSearchRequest request = new TextSearchRequest(context);
    request.pagetoken(nextPageToken);
    return request;
  }

  /**
   * Request the details of a Place.
   *
   * We are only enabling looking up Places by placeId as the older Place identifier - reference - is deprecated.
   * Please see the <a href="https://developers.google.com/places/web-service/details#deprecation">deprecation warning</a>.
   *
   * @param context The context on which to make Geo API requests.
   * @param placeId The PlaceID to request details on.
   * @return Returns a PlaceDetailsRequest that you can configure and execute.
   */

  public static PlaceDetailsRequest placeDetails(GeoApiContext context, String placeId) {
    PlaceDetailsRequest request = new PlaceDetailsRequest(context);
    request.placeId(placeId);
    return request;
  }

  /**
   * Request a Photo from a PhotoReference.
   *
   * @param context The context on which to make Geo API requests.
   * @param photoReference The reference to the photo to retrieve.
   * @return Returns a PhotoRequest that you can execute.
   */
  public static PhotoRequest photo(GeoApiContext context, String photoReference) {
    PhotoRequest request = new PhotoRequest(context);
    request.photoReference(photoReference);
    return request;
  }

  /**
   * Query Autocomplete allows you to add on-the-fly geographic query predictions to your application.
   *
   * @param context The context on which to make Geo API requests.
   * @param input input is the text string on which to search.
   * @return Returns a QueryAutocompleteRequest that you can configure and execute.
   */
  public static QueryAutocompleteRequest queryAutocomplete(GeoApiContext context, String input) {
    QueryAutocompleteRequest request = new QueryAutocompleteRequest(context);
    request.input(input);
    return request;
  }

}

