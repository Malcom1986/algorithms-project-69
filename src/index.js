// @ts-check

import _ from 'lodash';

const merge = (object1, object2) => {
  const keys = _.union(_.keys(object1), _.keys(object2));

  return keys.reduce((acc, key) => {
    if (_.has(object1, key) && _.has(object2, key)) {
      const value1 = object1[key];
      const value2 = object2[key];
      return { ...acc, [key]: _.flatten([value1, value2]) };
    }
    if (_.has(object1, key)) {
      const value = object1[key];
      const normalizedValue = _.isArray(value) ? value : [value];
      return { ...acc, [key]: normalizedValue };
    }
    const value = object2[key];
    const normalizedValue = _.isArray(value) ? value : [value];
    return { ...acc, [key]: normalizedValue };
  }, {});
};

const normalizeToken = (str) => {
  const matched = str.match(/\w+/g);
  if (matched) {
    return matched.join('').toLowerCase();
  }
  return null;
};

const buildInvertedIndex = (terms) => {
  const invertedIndex = terms.reduce((acc, term) => {
    const value = _.get(acc, term, 0);
    return { ...acc, [term]: value + 1 };
  }, {});
  return invertedIndex;
};

const calcIDF = (docsCount, termCount) => {
  const result = Math.log2(1 + (docsCount - termCount + 1) / (termCount + 0.5));
  return result;
};

const searchEngine = (documents, needle) => {
  const docsCount = documents.length;

  const docsTerms = documents.reduce((acc, doc) => {
    const { id, text } = doc;
    const lines = text.split('\n');
    const terms = lines.flatMap((line) => line.split(' ')).map(normalizeToken).filter((word) => word !== null);

    return { ...acc, [id]: terms };
  }, {});

  const invertedIndexes = documents.map((doc) => {
    const docTerms = docsTerms[doc.id];
    const docInvertedIndex = buildInvertedIndex(docTerms);
    return Object.keys(docInvertedIndex)
      .reduce((acc, term) => {
        const termCount = docInvertedIndex[term];
        const termFrequency = termCount / docTerms.length;
        return { ...acc, [term]: { docId: doc.id, termFrequency, count: termCount } };
      }, {});
  });

  const index = invertedIndexes.reduce((acc, documentIndex) => merge(acc, documentIndex), {});

  _.keys(index).forEach((term) => {
    const termDocs = index[term];
    const termDocsCount = termDocs.length;

    termDocs.forEach((doc) => {
      const { termFrequency } = doc;
      const docIdf = calcIDF(docsCount, termDocsCount);
      const tfIDF = termFrequency * docIdf;
      // eslint-disable-next-line no-param-reassign
      doc.tfIDF = tfIDF;
    });
  });

  const search = (text) => {
    const terms = text.split(' ').map(normalizeToken).filter((word) => word !== null);

    const currentIndex = _.pick(index, terms);

    const groupByDocId = _.groupBy(_.flatten(_.values(currentIndex)), 'docId');
    const currentDocsIds = _.keys(groupByDocId);
    const weightedDocs = currentDocsIds.reduce((acc, docId) => {
      const values = groupByDocId[docId];
      const sumIdf = _.sum(values.map((value) => value.tfIDF));
      return { ...acc, [docId]: sumIdf };
    }, {});

    return _.sortBy(currentDocsIds, (docId) => weightedDocs[docId]).reverse();
  };

  return search(needle);
};

export default searchEngine;
