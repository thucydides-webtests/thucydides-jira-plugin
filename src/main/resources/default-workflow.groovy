when 'Open', {
    'success' should: 'Resolve issue'
}

when 'Resolved', {
    'failure' should: 'Reopen issue'
}

when 'In Progress', {
    'success' should: ['Stop Progress','Resolve issue']
}